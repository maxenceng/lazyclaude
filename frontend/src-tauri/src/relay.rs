//! Spawns the sidecar and carries its lines both ways. It knows no JSON-RPC method: only that the first line is `ready`.

use std::ffi::OsString;
use std::io::{Read, Write};
use std::path::PathBuf;
use std::process::{Child, ChildStdin, Command, Stdio};
use std::sync::{Arc, Mutex, MutexGuard};
use std::thread;

use serde::Serialize;

use crate::lines::{Lines, Tail};

const READ_SIZE: usize = 64 * 1024;
const STDERR_LINES: usize = 20;

#[derive(Clone, Serialize)]
#[serde(tag = "kind", rename_all = "camelCase")]
pub enum RelayMessage {
    Lines { lines: Vec<String> },
    Exited(Exit),
}

#[derive(Clone, Serialize)]
pub struct Exit {
    pub code: Option<i32>,
    pub stderr: Vec<String>,
}

/// Where the page's messages go: the attached page's `Channel` in the window, a recorder in a test.
pub type OnMessage = Arc<dyn Fn(RelayMessage) + Send + Sync>;

/// The process the relay runs: `java -jar <jar> --sidecar` in the project directory, a stand-in in a test.
#[derive(Clone)]
pub struct Sidecar {
    program: OsString,
    args: Vec<OsString>,
    directory: PathBuf,
}

impl Sidecar {
    pub fn java(jar: PathBuf, project: PathBuf) -> Self {
        Self {
            program: "java".into(),
            args: vec![
                "--enable-native-access=ALL-UNNAMED".into(),
                "-jar".into(),
                jar.into_os_string(),
                "--sidecar".into(),
            ],
            directory: project,
        }
    }

    fn command(&self) -> Command {
        let mut command = Command::new(&self.program);
        command.args(&self.args).current_dir(&self.directory);
        command
    }

    fn describe(&self) -> String {
        let args: Vec<_> = self.args.iter().map(|arg| arg.to_string_lossy()).collect();
        format!("{} {}", self.program.to_string_lossy(), args.join(" "))
    }
}

/// What a page learns when it attaches: the `ready` line if it went by already, and the exit if the sidecar is gone.
#[derive(Serialize)]
pub struct Attached {
    pub ready: Option<String>,
    pub exit: Option<Exit>,
}

struct Running {
    generation: u64,
    channel: Option<OnMessage>,
    lines: Lines,
    stderr: Tail,
    exit: Option<Exit>,
    closing: bool,
    spawned: bool,
}

#[derive(Clone)]
pub struct Relay {
    sidecar: Sidecar,
    running: Arc<Mutex<Running>>,
    /// Apart from `running`, so a write blocked on a full pipe never stops stdout being drained. Taken after `running`, never before.
    stdin: Arc<Mutex<Option<ChildStdin>>>,
}

impl Relay {
    pub fn new(sidecar: Sidecar) -> Self {
        let running = Running {
            generation: 0,
            channel: None,
            lines: Lines::default(),
            stderr: Tail::new(STDERR_LINES),
            exit: None,
            closing: false,
            spawned: false,
        };
        Self {
            sidecar,
            running: Arc::new(Mutex::new(running)),
            stdin: Arc::new(Mutex::new(None)),
        }
    }

    pub fn attach(&self, channel: OnMessage) -> Attached {
        let first = {
            let mut running = self.lock();
            running.channel = Some(channel);
            let first = !running.spawned;
            running.spawned = true;
            first
        };
        if first {
            self.spawn();
        }
        let running = self.lock();
        Attached {
            ready: running.lines.first().map(str::to_owned),
            exit: running.exit.clone(),
        }
    }

    pub fn restart(&self, channel: OnMessage) -> Attached {
        {
            let mut running = self.lock();
            running.channel = Some(channel);
        }
        *self.stdin() = None;
        self.spawn();
        Attached {
            ready: None,
            exit: None,
        }
    }

    pub fn send(&self, line: &str) -> Result<(), String> {
        let mut stdin = self.stdin();
        let stdin = stdin.as_mut().ok_or("the sidecar is not running")?;
        stdin
            .write_all(line.as_bytes())
            .and_then(|()| stdin.write_all(b"\n"))
            .and_then(|()| stdin.flush())
            .map_err(|error| error.to_string())
    }

    /// Closing stdin is the only shutdown the sidecar knows.
    pub fn close(&self) {
        let mut running = self.lock();
        running.closing = true;
        *self.stdin() = None;
    }

    fn lock(&self) -> MutexGuard<'_, Running> {
        self.running
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
    }

    fn stdin(&self) -> MutexGuard<'_, Option<ChildStdin>> {
        self.stdin
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
    }

    fn spawn(&self) {
        let generation = {
            let mut running = self.lock();
            running.generation += 1;
            running.spawned = true;
            running.closing = false;
            running.exit = None;
            running.lines = Lines::default();
            running.stderr = Tail::new(STDERR_LINES);
            running.generation
        };
        let spawned = self
            .sidecar
            .command()
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn();
        match spawned {
            Ok(child) => self.watch(child, generation),
            Err(error) => {
                let reason = format!("cannot run {}: {error}", self.sidecar.describe());
                self.finish(generation, None, Some(reason));
            }
        }
    }

    fn watch(&self, mut child: Child, generation: u64) {
        *self.stdin() = child.stdin.take();
        let stdout = child.stdout.take();
        let stderr = child.stderr.take();
        let relay = self.clone();
        thread::spawn(move || {
            let stderr_reader = stderr.map(|pipe| {
                let relay = relay.clone();
                thread::spawn(move || relay.read_stderr(pipe, generation))
            });
            if let Some(pipe) = stdout {
                relay.read_stdout(pipe, generation);
            }
            if let Some(reader) = stderr_reader {
                let _ = reader.join();
            }
            let code = child.wait().ok().and_then(|status| status.code());
            relay.finish(generation, code, None);
        });
    }

    fn read_stdout(&self, mut pipe: impl Read, generation: u64) {
        let mut buffer = vec![0u8; READ_SIZE];
        while let Ok(count) = pipe.read(&mut buffer) {
            if count == 0 {
                break;
            }
            let mut running = self.lock();
            if running.generation != generation {
                break;
            }
            let lines = running.lines.feed(&buffer[..count]);
            if lines.is_empty() {
                continue;
            }
            if let Some(channel) = &running.channel {
                channel(RelayMessage::Lines { lines });
            }
        }
    }

    fn read_stderr(&self, mut pipe: impl Read, generation: u64) {
        let mut buffer = vec![0u8; READ_SIZE];
        let mut lines = Lines::default();
        while let Ok(count) = pipe.read(&mut buffer) {
            if count == 0 {
                break;
            }
            let mut running = self.lock();
            if running.generation != generation {
                break;
            }
            for line in lines.feed(&buffer[..count]) {
                running.stderr.push(line);
            }
        }
    }

    fn finish(&self, generation: u64, code: Option<i32>, reason: Option<String>) {
        let mut running = self.lock();
        if running.generation != generation || running.closing {
            return;
        }
        if let Some(reason) = reason {
            running.stderr.push(reason);
        }
        let exit = Exit {
            code,
            stderr: running.stderr.lines(),
        };
        *self.stdin() = None;
        running.exit = Some(exit.clone());
        if let Some(channel) = &running.channel {
            channel(RelayMessage::Exited(exit));
        }
    }
}

#[cfg(all(test, unix))]
mod tests {
    use super::*;
    use std::io::Cursor;
    use std::path::Path;
    use std::sync::mpsc::{self, Receiver};
    use std::time::{Duration, Instant};

    const WAIT: Duration = Duration::from_secs(10);
    const READY: &str = r#"{"jsonrpc":"2.0","method":"ready","params":{"protocol":1}}"#;
    const CHANGED: &str = r#"{"jsonrpc":"2.0","method":"board/changed","params":{}}"#;

    fn stand_in(script: &str, marker: &Path) -> Relay {
        Relay::new(Sidecar {
            program: "sh".into(),
            args: vec![
                "-c".into(),
                script.into(),
                "stand-in".into(),
                marker.as_os_str().to_owned(),
            ],
            directory: std::env::temp_dir(),
        })
    }

    fn recorder() -> (OnMessage, Receiver<RelayMessage>) {
        let (sender, receiver) = mpsc::channel();
        let sender = Mutex::new(sender);
        let on_message: OnMessage = Arc::new(move |message| {
            let _ = sender
                .lock()
                .unwrap_or_else(|poisoned| poisoned.into_inner())
                .send(message);
        });
        (on_message, receiver)
    }

    fn reading() -> (Relay, Receiver<RelayMessage>) {
        let relay = Relay::new(Sidecar {
            program: "unspawned".into(),
            args: Vec::new(),
            directory: std::env::temp_dir(),
        });
        let (on_message, receiver) = recorder();
        relay.lock().channel = Some(on_message);
        (relay, receiver)
    }

    fn lines_of(message: RelayMessage) -> Vec<String> {
        match message {
            RelayMessage::Lines { lines } => lines,
            RelayMessage::Exited(exit) => panic!("the stand-in exited: {:?} {:?}", exit.code, exit.stderr),
        }
    }

    fn lines(receiver: &Receiver<RelayMessage>) -> Vec<String> {
        lines_of(receiver.recv_timeout(WAIT).expect("a message from the relay"))
    }

    fn marker(name: &str) -> PathBuf {
        let path = std::env::temp_dir().join(format!("lazyclaude-relay-{name}-{}", std::process::id()));
        let _ = std::fs::remove_file(&path);
        path
    }

    fn appears(path: &Path) -> bool {
        let deadline = Instant::now() + WAIT;
        while Instant::now() < deadline {
            if path.exists() {
                return true;
            }
            thread::sleep(Duration::from_millis(20));
        }
        false
    }

    #[test]
    fn lines_read_together_reach_the_page_in_one_message_as_they_were_written() {
        let (relay, receiver) = reading();
        let stdout = format!("{READY}\n{CHANGED}\nthird\n");

        relay.read_stdout(Cursor::new(stdout), 0);

        let messages: Vec<_> = receiver.try_iter().map(lines_of).collect();
        assert_eq!(messages, vec![vec![READY, CHANGED, "third"]]);
    }

    #[test]
    fn a_line_cut_across_two_reads_reaches_the_page_once_whole_in_the_second_message() {
        let (relay, receiver) = reading();
        let stdout = format!("{READY}\n{CHANGED}\nthird\n");
        let (before, after) = stdout.split_at(READY.len() + "\n".len() + CHANGED.len() / 2);

        relay.read_stdout(Cursor::new(before.to_owned()).chain(Cursor::new(after.to_owned())), 0);

        let messages: Vec<_> = receiver.try_iter().map(lines_of).collect();
        assert_eq!(messages, vec![vec![READY], vec![CHANGED, "third"]]);
    }

    #[test]
    fn a_line_sent_reaches_the_sidecar_stdin_whole() {
        let script = format!("printf '%s\\n' '{READY}'; read line; printf 'echo:%s\\n' \"$line\"; read rest");
        let relay = stand_in(&script, &marker("send"));
        let (on_message, receiver) = recorder();
        relay.attach(on_message);
        assert_eq!(lines(&receiver), vec![READY]);

        relay.send(CHANGED).expect("the stand-in's stdin is open");

        assert_eq!(lines(&receiver), vec![format!("echo:{CHANGED}")]);
        relay.close();
    }

    #[test]
    fn a_page_attaching_later_is_handed_the_ready_line_kept_past_later_lines() {
        let script = format!("printf '%s\\n' '{READY}'; read line; printf 'later\\n'; read rest");
        let relay = stand_in(&script, &marker("reattach"));
        let (on_message, receiver) = recorder();
        relay.attach(on_message);
        assert_eq!(lines(&receiver), vec![READY]);
        relay.send("go").expect("the stand-in's stdin is open");
        assert_eq!(lines(&receiver), vec!["later"]);

        let (reloaded, _) = recorder();
        let attached = relay.attach(reloaded);

        assert_eq!(attached.ready.as_deref(), Some(READY));
        assert!(attached.exit.is_none());
        relay.close();
    }

    #[test]
    fn closing_the_relay_closes_the_sidecar_stdin() {
        let closed = marker("close");
        let relay = stand_in(&format!("printf '%s\\n' '{READY}'; cat >/dev/null; touch \"$1\""), &closed);
        let (on_message, receiver) = recorder();
        relay.attach(on_message);
        assert_eq!(lines(&receiver), vec![READY]);

        relay.close();

        assert!(appears(&closed), "the stand-in saw end of file on stdin and ran on to its marker");
        let _ = std::fs::remove_file(&closed);
    }

    #[test]
    fn an_unasked_exit_reaches_the_page_with_its_code_and_stderr() {
        let relay = stand_in("echo 'out of memory' >&2; exit 3", &marker("exit"));
        let (on_message, receiver) = recorder();

        relay.attach(on_message);

        match receiver.recv_timeout(WAIT).expect("a message from the relay") {
            RelayMessage::Exited(exit) => {
                assert_eq!(exit.code, Some(3));
                assert_eq!(exit.stderr, vec!["out of memory"]);
            }
            RelayMessage::Lines { lines } => panic!("unexpected lines {lines:?}"),
        }
    }
}

//! Spawns the sidecar and carries its lines both ways. It knows no JSON-RPC method: only that the first line is `ready`.

use std::io::{Read, Write};
use std::path::PathBuf;
use std::process::{Child, ChildStdin, Command, Stdio};
use std::sync::{Arc, Mutex, MutexGuard};
use std::thread;

use serde::Serialize;
use tauri::ipc::Channel;

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

/// What a page learns when it attaches: the `ready` line if it went by already, and the exit if the sidecar is gone.
#[derive(Serialize)]
pub struct Attached {
    pub ready: Option<String>,
    pub exit: Option<Exit>,
}

struct Running {
    generation: u64,
    channel: Option<Channel<RelayMessage>>,
    lines: Lines,
    stderr: Tail,
    exit: Option<Exit>,
    closing: bool,
    spawned: bool,
}

#[derive(Clone)]
pub struct Relay {
    jar: PathBuf,
    project: PathBuf,
    running: Arc<Mutex<Running>>,
    /// Apart from `running`, so a write blocked on a full pipe never stops stdout being drained. Taken after `running`, never before.
    stdin: Arc<Mutex<Option<ChildStdin>>>,
}

impl Relay {
    pub fn new(jar: PathBuf, project: PathBuf) -> Self {
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
            jar,
            project,
            running: Arc::new(Mutex::new(running)),
            stdin: Arc::new(Mutex::new(None)),
        }
    }

    pub fn attach(&self, channel: Channel<RelayMessage>) -> Attached {
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

    pub fn restart(&self, channel: Channel<RelayMessage>) -> Attached {
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
        let spawned = Command::new("java")
            .arg("--enable-native-access=ALL-UNNAMED")
            .arg("-jar")
            .arg(&self.jar)
            .arg("--sidecar")
            .current_dir(&self.project)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn();
        match spawned {
            Ok(child) => self.watch(child, generation),
            Err(error) => {
                let reason = format!("cannot run java on {}: {error}", self.jar.display());
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
                let _ = channel.send(RelayMessage::Lines { lines });
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
            let _ = channel.send(RelayMessage::Exited(exit));
        }
    }
}

package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.LazyClaude;

public final class SpawnedSidecar implements AutoCloseable {

    private static final String GH_SCRIPT = """
            #!/bin/sh
            if [ "$1" = "issue" ]; then
              cat <<'JSON'
            %s
            JSON
            elif [ "$1" = "pr" ] && [ "$2" = "diff" ]; then
              cat <<'DIFF'
            %s
            DIFF
            else
              echo "gh: not logged in" >&2
              exit 1
            fi
            """;
    private static final String DIFF = """
            diff --git a/src/Fixture.java b/src/Fixture.java
            index 1111111..2222222 100644
            --- a/src/Fixture.java
            +++ b/src/Fixture.java
            @@ -1,2 +1,2 @@
             kept
            -old
            +new""";
    private static final String ISSUES =
            "[{\"number\":7,\"title\":\"A fixture ticket\",\"body\":\"\",\"labels\":[{\"name\":\"status:todo\"},{\"name\":\"context:board\"},{\"name\":\"type:feature\"}],\"state\":\"OPEN\",\"stateReason\":\"\"}]";
    private static final String CLAUDE_SCRIPT = """
            #!/bin/bash
            echo $$ > %s
            stty raw -echo
            until [ "$(stty size)" != "0 0" ]; do sleep 0.01; done
            last=$(stty size)
            echo ready
            while true; do
              IFS= read -r -s -n1 -d '' -t 0.002 typed
              status=$?
              now=$(stty size)
              [ "$now" != "$last" ] && { last=$now; echo "size $now at ${EPOCHREALTIME/[.,]/}000"; }
              [ $status -gt 128 ] && continue
              [ "$typed" = "q" ] && exit 7
              printf '%%s' "$typed"
            done
            """;
    private static final Pattern RESIZE = Pattern.compile("size (\\d+ \\d+) at (\\d+)\\r?\\n");
    private static final long EXIT_PATIENCE_SECONDS = 30;
    private static final long LINE_PATIENCE_SECONDS = 30;
    private static final String END = "\u0000end of stdout";

    private final Process process;
    private final BlockingQueue<String> stdout = new LinkedBlockingQueue<>();
    private final BufferedWriter stdin;
    private final Path project;
    private final Path claudePid;
    private final Path state;
    private final List<String> held = new ArrayList<>();
    private final StringBuilder output = new StringBuilder();

    private SpawnedSidecar(Process process, Path project, Path claudePid, Path state) {
        this.process = process;
        this.project = project;
        this.claudePid = claudePid;
        this.state = state;
        Thread.ofPlatform().daemon().name("spawned-sidecar-stdout").start(this::readStdout);
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    public static SpawnedSidecar inFixtureProject(Path root) throws IOException, InterruptedException {
        Path project = Files.createDirectory(root.resolve("project"));
        Path home = Files.createDirectory(root.resolve("home"));
        Path bin = Files.createDirectory(root.resolve("bin"));
        Path gh = Files.writeString(bin.resolve("gh"), GH_SCRIPT.formatted(ISSUES, DIFF));
        Files.setPosixFilePermissions(gh, PosixFilePermissions.fromString("rwx------"));
        if (new ProcessBuilder("git", "init", "-q", project.toString()).inheritIO().start().waitFor() != 0) {
            throw new IllegalStateException("git init failed in " + project);
        }
        Path claudePid = root.resolve("claude.pid");
        Path claude = Files.writeString(bin.resolve("claude"), CLAUDE_SCRIPT.formatted(claudePid));
        Files.setPosixFilePermissions(claude, PosixFilePermissions.fromString("rwx------"));
        Path stderr = root.resolve("stderr.log");
        ProcessBuilder builder = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-Duser.home=" + home,
                        "-cp",
                        System.getProperty("java.class.path"),
                        LazyClaude.class.getName(),
                        "--sidecar")
                .directory(project.toFile())
                .redirectError(stderr.toFile());
        builder.environment().keySet().removeIf(name -> name.startsWith("XDG_") || name.startsWith("CLAUDE"));
        builder.environment().put("PATH", bin + ":" + System.getenv("PATH"));
        return new SpawnedSidecar(builder.start(), project.toRealPath(), claudePid, home.resolve(".local/state/lazyclaude"));
    }

    public static String protocolSchemaPrinted() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        LazyClaude.class.getName(),
                        "--protocol-schema")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        String printed = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!process.waitFor(EXIT_PATIENCE_SECONDS, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("--protocol-schema did not end cleanly");
        }
        return printed;
    }

    public String projectPath() {
        return project.toString();
    }

    public long claudePid() throws IOException {
        return Long.parseLong(Files.readString(claudePid).strip());
    }

    public boolean claudeIsAlive() throws IOException {
        return ProcessHandle.of(claudePid()).map(ProcessHandle::isAlive).orElse(false);
    }

    public List<String> launchLogLines() throws IOException {
        try (Stream<Path> logs = Files.list(state)) {
            return logs.flatMap(log -> readLines(log).stream()).toList();
        }
    }

    public String awaitLine(Predicate<String> wanted) throws InterruptedException {
        Optional<String> earlier = held.stream().filter(wanted).findFirst();
        if (earlier.isPresent()) {
            held.remove(earlier.get());
            return earlier.get();
        }
        String line = nextLine();
        while (!wanted.test(line)) {
            held.add(line);
            line = nextLine();
        }
        return line;
    }

    public String awaitResultOf(int id) throws InterruptedException {
        return awaitLine(line -> line.startsWith("{\"jsonrpc\":\"2.0\",\"id\":%d,".formatted(id)));
    }

    public String awaitOutputContaining(String text) throws InterruptedException {
        while (!output.toString().contains(text)) {
            awaitMoreOutput();
        }
        return output.toString();
    }

    private void awaitMoreOutput() throws InterruptedException {
        String line = awaitLine(pushed -> pushed.startsWith("{\"jsonrpc\":\"2.0\",\"method\":\"pty/output\""));
        output.append(new String(
                Base64.getDecoder().decode(SidecarJson.parse(line).path("params").path("data").asString()), StandardCharsets.UTF_8));
    }

    public String startedSession() throws InterruptedException, IOException {
        nextLine();
        send(request(1, "session/startUnlinked", "{\"root\":\"%s\",\"size\":{\"width\":100,\"height\":30}}".formatted(projectPath())));
        String session = SidecarJson.parse(awaitResultOf(1)).path("result").path("id").asString();
        awaitOutputContaining("ready");
        return session;
    }

    public List<StubResize> awaitResizes(int count) throws InterruptedException {
        while (resizesSeen().size() < count) {
            awaitMoreOutput();
        }
        return resizesSeen();
    }

    private List<StubResize> resizesSeen() {
        return RESIZE.matcher(output).results()
                .map(report -> new StubResize(report.group(1), Duration.ofNanos(Long.parseLong(report.group(2)))))
                .toList();
    }

    public String nextLine() throws InterruptedException {
        String line = stdout.poll(LINE_PATIENCE_SECONDS, TimeUnit.SECONDS);
        if (line == null || END.equals(line)) {
            throw new IllegalStateException("the sidecar wrote no line in time");
        }
        return line;
    }

    public void send(String line) throws IOException {
        stdin.write(line);
        stdin.newLine();
        stdin.flush();
    }

    public int closeStdinAndAwaitExit() throws IOException, InterruptedException {
        stdin.close();
        if (!process.waitFor(EXIT_PATIENCE_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("the sidecar did not end when stdin closed");
        }
        return process.exitValue();
    }

    public List<String> remainingLines() throws InterruptedException {
        List<String> lines = new ArrayList<>(held);
        String line = stdout.poll(LINE_PATIENCE_SECONDS, TimeUnit.SECONDS);
        while (line != null && !END.equals(line)) {
            lines.add(line);
            line = stdout.poll(LINE_PATIENCE_SECONDS, TimeUnit.SECONDS);
        }
        if (line == null) {
            throw new IllegalStateException("the sidecar's stdout did not end in time");
        }
        return lines;
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readStdout() {
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            lines.lines().forEach(stdout::add);
        } catch (IOException | UncheckedIOException e) {
            stdout.add(END);
            return;
        }
        stdout.add(END);
    }

    @Override
    public void close() {
        process.destroyForcibly().onExit().join();
    }
}

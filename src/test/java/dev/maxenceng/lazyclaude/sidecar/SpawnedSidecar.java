package dev.maxenceng.lazyclaude.sidecar;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import dev.maxenceng.lazyclaude.LazyClaude;

public final class SpawnedSidecar implements AutoCloseable {

    private static final String GH_SCRIPT = """
            #!/bin/sh
            if [ "$1" = "issue" ]; then
              cat <<'JSON'
            %s
            JSON
            else
              exit 1
            fi
            """;
    private static final String ISSUES =
            "[{\"number\":7,\"title\":\"A fixture ticket\",\"body\":\"\",\"labels\":[{\"name\":\"status:todo\"},{\"name\":\"context:board\"},{\"name\":\"type:feature\"}],\"state\":\"OPEN\",\"stateReason\":\"\"}]";
    private static final long EXIT_PATIENCE_SECONDS = 30;
    private static final long LINE_PATIENCE_SECONDS = 30;
    private static final String END = "\u0000end of stdout";

    private final Process process;
    private final BlockingQueue<String> stdout = new LinkedBlockingQueue<>();
    private final BufferedWriter stdin;
    private final Path project;

    private SpawnedSidecar(Process process, Path project) {
        this.process = process;
        this.project = project;
        Thread.ofPlatform().daemon().name("spawned-sidecar-stdout").start(this::readStdout);
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    public static SpawnedSidecar inFixtureProject(Path root) throws IOException, InterruptedException {
        Path project = Files.createDirectory(root.resolve("project"));
        Path home = Files.createDirectory(root.resolve("home"));
        Path bin = Files.createDirectory(root.resolve("bin"));
        Path gh = Files.writeString(bin.resolve("gh"), GH_SCRIPT.formatted(ISSUES));
        Files.setPosixFilePermissions(gh, PosixFilePermissions.fromString("rwx------"));
        if (new ProcessBuilder("git", "init", "-q", project.toString()).inheritIO().start().waitFor() != 0) {
            throw new IllegalStateException("git init failed in " + project);
        }
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
        builder.environment().keySet().removeIf(name -> name.startsWith("XDG_"));
        builder.environment().put("PATH", bin + ":" + System.getenv("PATH"));
        return new SpawnedSidecar(builder.start(), project.toRealPath());
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
        List<String> lines = new ArrayList<>();
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

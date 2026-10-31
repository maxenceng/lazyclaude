package dev.maxenceng.lazyclaude.sidecar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
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

    private final Process process;
    private final BufferedReader stdout;
    private final BufferedWriter stdin;
    private final Path project;
    private final Path stderr;

    private SpawnedSidecar(Process process, Path project, Path stderr) {
        this.process = process;
        this.project = project;
        this.stderr = stderr;
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    public static SpawnedSidecar inFixtureProject() throws IOException, InterruptedException {
        Path root = Files.createTempDirectory("sidecar-protocol");
        Path project = Files.createDirectory(root.resolve("project"));
        Path home = Files.createDirectory(root.resolve("home"));
        Path bin = Files.createDirectory(root.resolve("bin"));
        Path gh = Files.writeString(bin.resolve("gh"), GH_SCRIPT.formatted(ISSUES));
        Files.setPosixFilePermissions(gh, PosixFilePermissions.fromString("rwx------"));
        new ProcessBuilder("git", "init", "-q", project.toString()).inheritIO().start().waitFor();
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
        builder.environment().put("PATH", bin + ":" + System.getenv("PATH"));
        return new SpawnedSidecar(builder.start(), project.toRealPath(), stderr);
    }

    public String projectPath() {
        return project.toString();
    }

    public String nextLine() throws IOException {
        return stdout.readLine();
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

    public List<String> remainingLines() throws IOException {
        return stdout.lines().toList();
    }

    public String stderrText() throws IOException {
        return Files.readString(stderr);
    }

    @Override
    public void close() {
        process.destroyForcibly();
    }
}

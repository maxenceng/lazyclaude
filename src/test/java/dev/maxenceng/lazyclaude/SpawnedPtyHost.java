package dev.maxenceng.lazyclaude;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.awaited;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.exitOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.newLaunch;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostRepository;

public final class SpawnedPtyHost implements AutoCloseable {

    private static final String READY = "ready";
    private static final String CLAUDE_SCRIPT = "#!/bin/sh\nexit 0\n";
    private static final long PATIENCE_SECONDS = 30;

    private final Process process;
    private final BufferedReader stdout;
    private final Path stderr;

    private SpawnedPtyHost(Process process, Path stderr) {
        this.process = process;
        this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.stderr = stderr;
    }

    public static void main(String[] arguments) throws Exception {
        PtyHostRepository host = new ConfigurationLoader(Path.of(System.getProperty("user.home")), System.getenv()).host();
        System.out.println(READY);
        System.out.flush();
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        CompletableFuture<ExitCode> exit = exitOf();
        host.spawn(newLaunch(sessionId()), bytes -> {}, exit::complete);
        System.exit(awaited(exit).value());
    }

    public static SpawnedPtyHost started(Path root, String name, Path cache) throws IOException {
        Path own = Files.createDirectories(root.resolve(name));
        Path bin = Files.createDirectory(own.resolve("bin"));
        Path claude = Files.writeString(bin.resolve("claude"), CLAUDE_SCRIPT);
        Files.setPosixFilePermissions(claude, PosixFilePermissions.fromString("rwx------"));
        Path stderr = own.resolve("stderr.log");
        ProcessBuilder builder = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-Duser.home=" + Files.createDirectory(own.resolve("home")),
                        "-Djava.io.tmpdir=" + tmpdirOf(root, name),
                        "-cp",
                        System.getProperty("java.class.path"),
                        SpawnedPtyHost.class.getName())
                .redirectError(stderr.toFile());
        builder.environment().keySet().removeIf(variable -> variable.startsWith("XDG_") || variable.startsWith("CLAUDE"));
        builder.environment().put("XDG_CACHE_HOME", cache.toString());
        builder.environment().put("PATH", bin + ":" + System.getenv("PATH"));
        return new SpawnedPtyHost(builder.start(), stderr);
    }

    public static Path tmpdirOf(Path root, String name) throws IOException {
        return Files.createDirectories(root.resolve(name).resolve("tmp"));
    }

    public static List<Path> pty4jFoldersIn(Path tmpdir) throws IOException {
        try (Stream<Path> entries = Files.list(tmpdir)) {
            return entries.filter(entry -> entry.getFileName().toString().startsWith("pty4j-")).toList();
        }
    }

    public static Path extractedLibraryIn(Path cache) {
        return cache.resolve("lazyclaude").resolve("pty4j").resolve("pty4j-linux-x86-64").resolve("libpty.so");
    }

    public void awaitReady() throws IOException {
        String line = stdout.readLine();
        if (!READY.equals(line)) {
            throw new IllegalStateException("the child did not get ready: " + Files.readString(stderr));
        }
    }

    public void go() throws IOException {
        OutputStream stdin = process.getOutputStream();
        stdin.write('\n');
        stdin.flush();
    }

    public int awaitExit() throws IOException, InterruptedException {
        if (!process.waitFor(PATIENCE_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("the child did not end in time");
        }
        return process.exitValue();
    }

    public String stderr() throws IOException {
        return Files.readString(stderr);
    }

    @Override
    public void close() {
        process.destroyForcibly().onExit().join();
    }
}

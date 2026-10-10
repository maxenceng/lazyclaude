package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class SessionWatchFixture {

    private static final Duration SHORT_BATCH = Duration.ofMillis(100);
    private static final Duration SHORT_SWEEP = Duration.ofMillis(200);
    private static final Duration REWRITE_PAUSE = Duration.ofMillis(20);
    private static final Duration LONG_SWEEP = Duration.ofMinutes(5);

    private SessionWatchFixture() {}

    public static Duration within() {
        return Duration.ofSeconds(5);
    }

    public static Duration settled() {
        return Duration.ofMillis(700);
    }

    static SessionWatchRepository watching(SessionWatchRepository watcher, ChangeProbe probe) {
        watcher.watch(probe.listener());
        return watcher;
    }

    public static SessionWatchRepository watcherWithoutSweep(Path sessions, ByteArrayOutputStream said) {
        return new SessionWatchRepository(
                sessions, () -> FileSystems.getDefault().newWatchService(), warnings(said), SHORT_BATCH, LONG_SWEEP);
    }

    public static SessionWatchRepository sweepingWatcher(Path sessions, ByteArrayOutputStream said) {
        return new SessionWatchRepository(
                sessions, () -> FileSystems.getDefault().newWatchService(), warnings(said), SHORT_BATCH, SHORT_SWEEP);
    }

    public static SessionWatchRepository watcherWithoutInotify(Path sessions, ByteArrayOutputStream said) {
        return new SessionWatchRepository(
                sessions,
                () -> {
                    throw new IOException("User limit of inotify watches reached");
                },
                warnings(said),
                SHORT_BATCH,
                SHORT_SWEEP);
    }

    public static AutoCloseable rewritingWithoutPause(Path file) {
        Thread writer = Thread.ofPlatform().daemon().start(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Files.writeString(file, Long.toString(System.nanoTime()), StandardCharsets.UTF_8);
                    Thread.sleep(REWRITE_PAUSE);
                }
            } catch (IOException | InterruptedException stopped) {
                Thread.currentThread().interrupt();
            }
        });
        return writer::interrupt;
    }

    private static PrintStream warnings(ByteArrayOutputStream said) {
        return new PrintStream(said, true, StandardCharsets.UTF_8);
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import dev.maxenceng.lazyclaude.session.domain.WatchFallback;

public final class SessionWatchFixture {

    private static final Duration SHORT_BATCH = Duration.ofMillis(100);
    private static final Duration SHORT_SWEEP = Duration.ofMillis(200);
    private static final Duration REWRITE_PAUSE = Duration.ofMillis(20);
    private static final Duration LONG_SWEEP = Duration.ofMinutes(5);
    private static final Set<Long> HOSTED = Set.of(17234L, 22862L);

    private SessionWatchFixture() {}

    public static Duration within() {
        return Duration.ofSeconds(5);
    }

    public static Duration sweepInterval() {
        return SHORT_SWEEP;
    }

    public static Duration settled() {
        return Duration.ofMillis(700);
    }

    static SessionWatchRepository watching(SessionWatchRepository watcher, ChangeProbe probe, WatchFallback fallback) {
        watcher.watch(pid -> HOSTED.contains(pid.value()), probe.listener(), fallback);
        return watcher;
    }

    public static SessionWatchRepository watcherWithoutSweep(Path sessions) {
        return new SessionWatchRepository(
                sessions, () -> FileSystems.getDefault().newWatchService(), SHORT_BATCH, LONG_SWEEP);
    }

    public static SessionWatchRepository sweepingWatcher(Path sessions) {
        return new SessionWatchRepository(
                sessions, () -> FileSystems.getDefault().newWatchService(), SHORT_BATCH, SHORT_SWEEP);
    }

    public static SessionWatchRepository watcherWithoutInotify(Path sessions) {
        return new SessionWatchRepository(
                sessions,
                () -> {
                    throw new IOException("User limit of inotify watches reached");
                },
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
}

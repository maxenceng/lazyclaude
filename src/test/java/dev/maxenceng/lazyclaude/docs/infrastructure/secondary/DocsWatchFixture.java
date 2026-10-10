package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class DocsWatchFixture {

    private static final Duration SHORT_BATCH = Duration.ofMillis(150);
    private static final Duration SHORT_POLL = Duration.ofMillis(200);

    private DocsWatchFixture() {}

    public static Duration within() {
        return Duration.ofSeconds(5);
    }

    public static Duration settled() {
        return Duration.ofMillis(700);
    }

    static DocsWatchRepository watching(DocsWatchRepository watcher, Path root, ChangeProbe probe) {
        watcher.watch(new Project(root), probe.listener());
        return watcher;
    }

    public static DocsWatchRepository watcher(ByteArrayOutputStream said) {
        return new DocsWatchRepository(
                () -> FileSystems.getDefault().newWatchService(),
                new PrintStream(said, true, StandardCharsets.UTF_8),
                SHORT_BATCH,
                SHORT_POLL);
    }

    public static DocsWatchRepository watcherWithoutInotify(ByteArrayOutputStream said) {
        return new DocsWatchRepository(
                () -> {
                    throw new IOException("User limit of inotify watches reached");
                },
                new PrintStream(said, true, StandardCharsets.UTF_8),
                SHORT_BATCH,
                SHORT_POLL);
    }
}

package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.time.Duration;

import dev.maxenceng.lazyclaude.docs.domain.WatchFallback;
import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class DocsWatchFixture {

    private static final DirectoryRegistrar REAL_REGISTRAR = (service, directory) ->
            directory.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
    private static final Duration SHORT_BATCH = Duration.ofMillis(150);
    private static final Duration SHORT_POLL = Duration.ofMillis(200);

    private DocsWatchFixture() {}

    public static Duration within() {
        return Duration.ofSeconds(5);
    }

    public static Duration pollInterval() {
        return SHORT_POLL;
    }

    public static Duration settled() {
        return Duration.ofMillis(700);
    }

    static DocsWatchRepository watching(DocsWatchRepository watcher, Path root, ChangeProbe probe, WatchFallback fallback) {
        watcher.watch(new Project(root), probe.listener(), fallback);
        return watcher;
    }

    public static DocsWatchRepository watcher() {
        return new DocsWatchRepository(
                () -> FileSystems.getDefault().newWatchService(),
                REAL_REGISTRAR,
                SHORT_BATCH,
                SHORT_POLL);
    }

    public static DocsWatchRepository watcherWithoutInotify() {
        return new DocsWatchRepository(
                () -> {
                    throw new IOException("User limit of inotify watches reached");
                },
                REAL_REGISTRAR,
                SHORT_BATCH,
                SHORT_POLL);
    }

    static DocsWatchRepository watcherOver(FakeWatchService service, DirectoryRegistrar registrar) {
        return new DocsWatchRepository(
                () -> service, registrar, SHORT_BATCH, SHORT_POLL);
    }

    static DocsWatchRepository watcherRegisteringWith(DirectoryRegistrar registrar) {
        return new DocsWatchRepository(
                () -> FileSystems.getDefault().newWatchService(),
                registrar,
                SHORT_BATCH,
                SHORT_POLL);
    }
}

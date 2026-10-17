package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.docs.domain.WatchFallback;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ChangeProbe;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture;

public final class DocsWatchFixture {

    private DocsWatchFixture() {}

    static DocsWatchRepository watching(DocsWatchRepository watcher, Path root, ChangeProbe probe, WatchFallback fallback) {
        watcher.watch(new Project(root), probe.listener(), fallback);
        return watcher;
    }

    public static DocsWatchRepository watcher() {
        return new DocsWatchRepository(TreeWatchFixture.watcher());
    }

    public static DocsWatchRepository watcherWithoutInotify() {
        return new DocsWatchRepository(TreeWatchFixture.watcherWithoutInotify());
    }
}

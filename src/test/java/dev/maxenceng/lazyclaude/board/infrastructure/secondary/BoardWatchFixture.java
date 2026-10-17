package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.domain.WatchFallback;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ChangeProbe;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatcher;

public final class BoardWatchFixture {

    private BoardWatchFixture() {}

    static BoardWatchRepository watching(TreeWatcher tree, Path root, ChangeProbe probe, WatchFallback fallback) {
        BoardWatchRepository watcher = new BoardWatchRepository(tree);
        watcher.watch(new Project(root), probe.listener(), fallback);
        return watcher;
    }
}

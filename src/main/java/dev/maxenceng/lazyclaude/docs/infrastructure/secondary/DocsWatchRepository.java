package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.docs.domain.WatchFallback;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatcher;

/** Watches {@code docs/} except {@code docs/backlog/} on a {@link TreeWatcher}, and tells the fallback when the tree is polled instead. */
public final class DocsWatchRepository implements DocsWatchPort, AutoCloseable {

    private static final Path DOCS = Path.of("docs");

    private final TreeWatcher watcher;

    public DocsWatchRepository() {
        this(new TreeWatcher());
    }

    DocsWatchRepository(TreeWatcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public void watch(Project project, Runnable changed, WatchFallback fallback) {
        Path docs = project.root().resolve(DOCS);
        // deferred: a project without docs/ at start is never watched, so a docs/ created later shows up only after a restart — close it when someone starts a project that way.
        if (!Files.isDirectory(docs)) {
            return;
        }
        watcher.start(changed, (_, cause, every) -> fallback.polling(new Reason(cause), every));
        watcher.add(new DocsScope(docs));
    }

    @Override
    public void close() {
        watcher.close();
    }
}

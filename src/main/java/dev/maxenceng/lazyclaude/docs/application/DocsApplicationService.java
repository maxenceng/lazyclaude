package dev.maxenceng.lazyclaude.docs.application;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.docs.domain.DocsManager;
import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsTree;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads a project's docs tree, and keeps a listener handed a fresh one as the docs change. */
public final class DocsApplicationService {

    private final DocsManager manager;

    public DocsApplicationService(DocsPort docs, DocsWatchPort watch) {
        this.manager = new DocsManager(docs, watch);
    }

    public DocsTree docsFor(Project project) {
        return manager.docsFor(project);
    }

    public void subscribe(Project project, Consumer<DocsTree> listener) {
        manager.watch(project, () -> {
            try {
                listener.accept(manager.docsFor(project));
            } catch (DomainException unavailable) {
                // deferred: a docs directory that cannot be listed is not reported while running, the previous tree stays — close it when the shell has a place to say so.
            }
        });
    }
}

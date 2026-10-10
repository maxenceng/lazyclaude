package dev.maxenceng.lazyclaude.docs.application;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.docs.domain.Docs;
import dev.maxenceng.lazyclaude.docs.domain.DocsManager;
import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads a project's docs, and keeps a listener handed them again as they change. */
public final class DocsApplicationService {

    private final DocsManager manager;

    public DocsApplicationService(DocsPort docs, DocsWatchPort watch) {
        this.manager = new DocsManager(docs, watch);
    }

    public Docs docsFor(Project project) {
        return manager.docsFor(project);
    }

    public void subscribe(Project project, Consumer<Docs> listener) {
        manager.watch(project, () -> listener.accept(manager.docsFor(project)));
    }
}

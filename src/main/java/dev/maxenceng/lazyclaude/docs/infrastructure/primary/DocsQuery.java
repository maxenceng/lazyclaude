package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.docs.application.DocsApplicationService;

/** What a frontend asks of `docs`: a project's documents as a view, and a view again whenever they change. It only reads. */
public final class DocsQuery {

    private final DocsApplicationService docs;

    public DocsQuery(DocsApplicationService docs) {
        this.docs = docs;
    }

    /** The project's docs tree, or why its {@code docs/} cannot be listed; never a failure. */
    public DocsView docsFor(DocsPayload payload) {
        return DocsView.from(docs.docsFor(payload.toDomain()));
    }

    /** Hands {@code listener} a view after each change, on a thread of the watcher's own, not the caller's. */
    public void subscribe(DocsPayload payload, Consumer<DocsView> listener) {
        docs.subscribe(payload.toDomain(), read -> listener.accept(DocsView.from(read)));
    }
}

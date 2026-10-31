package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;

import tools.jackson.databind.JsonNode;

/** The {@code docs/} methods: {@code docs/docsFor} for the read of {@link DocsQuery}, {@code docs/watch} for its subscription, which pushes {@code docs/changed} (ADR 0050). */
final class DocsMethods {

    private static final ProtocolMethod<DocsPayload, DocsView> DOCS_FOR =
            new ProtocolMethod<>("docs/docsFor", DocsPayload.class, DocsView.class);
    private static final ProtocolMethod<DocsPayload, DocsView> WATCH = new ProtocolMethod<>("docs/watch", DocsPayload.class, DocsView.class);
    private static final ProtocolNotification<DocsView> CHANGED = new ProtocolNotification<>("docs/changed", DocsView.class);

    private final DocsQuery docs;
    private final SidecarOutput output;
    private final Set<Path> watching = ConcurrentHashMap.newKeySet();

    DocsMethods(DocsQuery docs, SidecarOutput output) {
        this.docs = docs;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(List.of(DOCS_FOR, WATCH), List.of(), List.of(CHANGED));
    }

    Map<String, SidecarMethod> methods() {
        return Map.of(DOCS_FOR.name(), this::docsFor, WATCH.name(), this::watch);
    }

    private SidecarAnswer docsFor(JsonNode params) {
        return SidecarAnswer.of(SidecarJson.tree(DOCS_FOR.answer(params, docs::docsFor)));
    }

    /** The watch opens once the current docs are queued, so no {@code docs/changed} reaches the frontend before the docs it changes. */
    // deferred: a change between this read and the watch starting is pushed only at the next change — close it when a document edited as the frontend connects is seen stale.
    private SidecarAnswer watch(JsonNode params) {
        DocsView current = WATCH.answer(params, docs::docsFor);
        DocsPayload payload = SidecarJson.params(params, WATCH.params());
        return new SidecarAnswer(SidecarJson.tree(current), () -> watchOnce(payload));
    }

    /** A watch that failed to open is forgotten before another {@code docs/watch} of the project is let in, so that one tries again. */
    private synchronized void watchOnce(DocsPayload payload) {
        if (watching.add(payload.root())) {
            try {
                docs.subscribe(payload, view -> output.send(CHANGED.message(view)));
            } catch (RuntimeException failure) {
                watching.remove(payload.root());
                throw failure;
            }
        }
    }
}

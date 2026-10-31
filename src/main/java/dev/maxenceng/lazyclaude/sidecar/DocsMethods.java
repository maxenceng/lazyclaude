package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;
import java.util.Map;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;

import tools.jackson.databind.JsonNode;

/** The {@code docs/} methods: {@code docs/docsFor} for the read of {@link DocsQuery}, {@code docs/watch} for its subscription, which pushes {@code docs/changed} (ADR 0050). */
final class DocsMethods {

    private static final ServedMethod<DocsQuery, DocsPayload, DocsView> DOCS_FOR =
            ServedMethod.of("docs/docsFor", DocsPayload.class, DocsView.class, DocsQuery::docsFor);
    private static final ProtocolMethod<DocsPayload, DocsView> WATCH = new ProtocolMethod<>("docs/watch", DocsPayload.class, DocsView.class);
    private static final ProtocolNotification<DocsView> CHANGED = new ProtocolNotification<>("docs/changed", DocsView.class);
    private static final Runnable ALREADY_PUSHING = () -> {};

    private final DocsQuery docs;
    private final SidecarOutput output;
    private final WatchedRoot watched = new WatchedRoot(WATCH.name());

    DocsMethods(DocsQuery docs, SidecarOutput output) {
        this.docs = docs;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(List.of(DOCS_FOR.method(), WATCH), List.of(), List.of(CHANGED));
    }

    Map<String, SidecarMethod> methods() {
        return Map.ofEntries(DOCS_FOR.boundTo(docs), Map.entry(WATCH.name(), this::watch));
    }

    /** The watch opens once the current docs are queued, so no {@code docs/changed} reaches the frontend before the docs it changes. */
    // deferred: a change between this read and the watch starting is pushed only at the next change — close it when a document edited as the frontend connects is seen stale.
    private SidecarAnswer watch(JsonNode params) {
        DocsPayload payload = SidecarJson.params(params, WATCH.params());
        watched.claim(payload.root());
        DocsView current = WATCH.answer(params, docs::docsFor);
        return new SidecarAnswer(
                SidecarJson.tree(current),
                () -> watched.open(() -> docs.subscribe(payload, view -> output.send(CHANGED.message(view))), ALREADY_PUSHING));
    }
}

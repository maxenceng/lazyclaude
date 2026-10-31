package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQuery;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningsView;

import tools.jackson.databind.JsonNode;

/** The {@code warning/} methods: {@code warning/watch} for the operations of {@link WarningQuery}, which push {@code warning/changed} (ADR 0050). */
final class WarningMethods {

    private static final ProtocolMethod<NoParams, WarningsView> WATCH =
            new ProtocolMethod<>("warning/watch", NoParams.class, WarningsView.class);
    private static final ProtocolNotification<WarningsView> CHANGED =
            new ProtocolNotification<>("warning/changed", WarningsView.class);

    private final WarningQuery warnings;
    private final SidecarOutput output;
    private final AtomicBoolean watching = new AtomicBoolean();

    WarningMethods(WarningQuery warnings, SidecarOutput output) {
        this.warnings = warnings;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(List.of(WATCH), List.of(), List.of(CHANGED));
    }

    Map<String, SidecarMethod> methods() {
        return Map.of(WATCH.name(), this::watch);
    }

    /**
     * The watch opens once the warnings held are queued, so no {@code warning/changed} reaches the frontend before the warnings it
     * changes; it hands what is held at once, so a warning withdrawn between the answer and the watch is still told.
     */
    private SidecarAnswer watch(JsonNode params) {
        return new SidecarAnswer(SidecarJson.tree(WATCH.answer(params, _ -> warnings.held())), this::watchOnce);
    }

    private void watchOnce() {
        if (watching.compareAndSet(false, true)) {
            try {
                warnings.watch(view -> output.send(CHANGED.message(view)));
            } catch (RuntimeException failure) {
                watching.set(false);
                throw failure;
            }
        }
    }
}

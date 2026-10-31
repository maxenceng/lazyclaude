package dev.maxenceng.lazyclaude.sidecar;

import java.util.function.Consumer;

import tools.jackson.databind.JsonNode;

/**
 * A call the sidecar takes and never answers, as the schema states it: the Payload its params are read into (ADR 0050). The frontend
 * sends it as a notification; sent as a request, it is answered with a null result. Its handler runs through {@link #accept}.
 */
record ProtocolInput<P>(String name, Class<P> params) {

    void accept(JsonNode call, Consumer<? super P> operation) {
        operation.accept(SidecarJson.params(call, params));
    }
}

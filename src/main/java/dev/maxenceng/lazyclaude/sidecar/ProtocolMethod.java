package dev.maxenceng.lazyclaude.sidecar;

import java.util.function.Function;

import tools.jackson.databind.JsonNode;

/**
 * A protocol method as the schema states it: the Payload its params are read into and the View type it answers with (ADR 0073). Its
 * handler answers through {@link #answer}, so an operation returning anything but the declared View does not compile.
 */
record ProtocolMethod<P, R>(String name, Class<P> params, Class<R> result) {

    R answer(JsonNode call, Function<? super P, ? extends R> operation) {
        return operation.apply(SidecarJson.params(call, params));
    }
}

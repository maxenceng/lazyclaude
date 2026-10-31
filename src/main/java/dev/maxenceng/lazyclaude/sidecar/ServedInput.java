package dev.maxenceng.lazyclaude.sidecar;

import java.util.Map;
import java.util.function.BiConsumer;

import tools.jackson.databind.node.NullNode;

/**
 * An input taken by one operation of a {@code T}, declared once: the schema reads its {@link ProtocolInput} without a {@code T} being
 * built, and the sidecar binds the operation to the one it serves. Sent as a request, it is answered with a null result.
 */
record ServedInput<T, P>(ProtocolInput<P> input, BiConsumer<? super T, ? super P> operation) {

    static <T, P> ServedInput<T, P> of(String name, Class<P> params, BiConsumer<? super T, ? super P> operation) {
        return new ServedInput<>(new ProtocolInput<>(name, params), operation);
    }

    Map.Entry<String, SidecarMethod> boundTo(T target) {
        return Map.entry(input.name(), params -> {
            input.accept(params, payload -> operation.accept(target, payload));
            return SidecarAnswer.of(NullNode.getInstance());
        });
    }
}

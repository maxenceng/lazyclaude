package dev.maxenceng.lazyclaude.sidecar;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * A method answered by one operation of a {@code T}, declared once: the schema reads its {@link ProtocolMethod} without a {@code T}
 * being built, and the sidecar binds the operation to the one it serves.
 */
record ServedMethod<T, P, R>(ProtocolMethod<P, R> method, BiFunction<? super T, ? super P, ? extends R> operation) {

    static <T, P, R> ServedMethod<T, P, R> of(
            String name, Class<P> params, Class<R> result, BiFunction<? super T, ? super P, ? extends R> operation) {
        return new ServedMethod<>(new ProtocolMethod<>(name, params, result), operation);
    }

    Map.Entry<String, SidecarMethod> boundTo(T target) {
        return Map.entry(
                method.name(),
                params -> SidecarAnswer.of(SidecarJson.tree(method.answer(params, payload -> operation.apply(target, payload)))));
    }
}

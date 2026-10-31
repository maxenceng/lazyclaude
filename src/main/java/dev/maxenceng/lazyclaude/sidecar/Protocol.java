package dev.maxenceng.lazyclaude.sidecar;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** What a group of methods declares, or the whole sidecar once the groups are merged: what the schema states and what is served (ADR 0073). */
record Protocol(List<ProtocolMethod<?, ?>> methods, List<ProtocolInput<?>> inputs, List<ProtocolNotification<?>> notifications) {

    static Protocol of(Protocol... groups) {
        return new Protocol(merged(groups, Protocol::methods), merged(groups, Protocol::inputs), merged(groups, Protocol::notifications));
    }

    Set<String> inputNames() {
        return inputs.stream().map(ProtocolInput::name).collect(Collectors.toUnmodifiableSet());
    }

    private static <T> List<T> merged(Protocol[] groups, Function<Protocol, List<T>> declared) {
        return Arrays.stream(groups).flatMap(group -> declared.apply(group).stream()).toList();
    }
}

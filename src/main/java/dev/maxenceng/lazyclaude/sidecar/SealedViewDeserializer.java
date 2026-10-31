package dev.maxenceng.lazyclaude.sidecar;

import java.util.Arrays;
import java.util.stream.Stream;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads a sealed type from its {@code kind}, as the writer writes it and the schema states it: the variant whose
 * {@link ViewShape#kindOf} is the {@code kind} reads the rest (ADR 0050). A {@code kind} no variant has is the caller's invalid param.
 */
final class SealedViewDeserializer extends ValueDeserializer<Object> {

    private static final String NO_VARIANT = "%s has no variant of kind %s";

    private final Class<?> type;

    SealedViewDeserializer(Class<?> type) {
        this.type = type;
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode node = context.readTree(parser);
        String kind = node.path(ViewShape.KIND).asString();
        Class<?> variant = variants(type)
                .filter(candidate -> ViewShape.kindOf(candidate).equals(kind))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(NO_VARIANT.formatted(type.getSimpleName(), kind)));
        return context.readTreeAsValue(withoutKind(node), variant);
    }

    private static Stream<Class<?>> variants(Class<?> sealed) {
        return Arrays.stream(sealed.getPermittedSubclasses()).flatMap(permitted -> permitted.isSealed() ? variants(permitted) : Stream.of(permitted));
    }

    private static JsonNode withoutKind(JsonNode node) {
        ObjectNode rest = SidecarJson.object();
        node.properties().stream().filter(property -> !property.getKey().equals(ViewShape.KIND)).forEach(property -> rest.set(property.getKey(), property.getValue()));
        return rest;
    }
}

package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads a sealed type's variant from an object led by its own {@code kind}, as the writer writes it and the schema requires it, wherever
 * the declared type is the sealed type or the variant itself: a {@code kind} missing or another variant's is the caller's invalid param
 * (ADR 0076). The rest is read by the default reader.
 */
final class VariantRecordDeserializer extends DelegatingDeserializer {

    private static final String WRONG_KIND = "%s is of kind %s, not %s";

    VariantRecordDeserializer(Class<?> type, ValueDeserializer<?> asObject) {
        super(asObject);
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> asObject) {
        return new VariantRecordDeserializer(handledType(), asObject);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode node = context.readTree(parser);
        String expected = ViewShape.kindOf(handledType());
        String kind = node.path(ViewShape.KIND).asString();
        if (!expected.equals(kind)) {
            return context.reportInputMismatch(handledType(), WRONG_KIND.formatted(handledType().getSimpleName(), expected, kind));
        }
        JsonParser rest = withoutKind(node).traverse(context);
        rest.nextToken();
        return super.deserialize(rest, context);
    }

    private static JsonNode withoutKind(JsonNode node) {
        ObjectNode rest = SidecarJson.object();
        node.properties().stream().filter(property -> !property.getKey().equals(ViewShape.KIND)).forEach(property -> rest.set(property.getKey(), property.getValue()));
        return rest;
    }
}

package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** The JSON Schema nodes the protocol schema is built of: a typed value, a constant, and an object that takes exactly its properties. */
final class SchemaNodes {

    private static final String TYPE = "type";
    private static final String OBJECT = "object";
    private static final String CONST = "const";
    private static final String PROPERTIES = "properties";
    private static final String REQUIRED = "required";
    private static final String ADDITIONAL_PROPERTIES = "additionalProperties";

    private SchemaNodes() {}

    static ObjectNode typed(String type) {
        ObjectNode node = SidecarJson.object();
        node.put(TYPE, type);
        return node;
    }

    static ObjectNode constant(String value) {
        ObjectNode node = SidecarJson.object();
        node.put(CONST, value);
        return node;
    }

    static ObjectNode constant(int value) {
        ObjectNode node = SidecarJson.object();
        node.put(CONST, value);
        return node;
    }

    /** An object of every property given, each required and no other allowed. */
    static ObjectNode closedObject(ObjectNode properties) {
        ObjectNode object = typed(OBJECT);
        object.set(PROPERTIES, properties);
        object.set(REQUIRED, names(properties));
        object.put(ADDITIONAL_PROPERTIES, false);
        return object;
    }

    private static ArrayNode names(ObjectNode properties) {
        return SidecarJson.texts(List.copyOf(properties.propertyNames()));
    }
}

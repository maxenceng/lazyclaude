package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The JSON Schema of the protocol (ADR 0073): every method with its params and result, every input with its params, and every
 * notification with its params, each type stated by {@link TypeSchema} and the named ones gathered under {@code $defs}.
 */
final class ProtocolSchema {

    private static final String SCHEMA_URI = "https://json-schema.org/draft/2020-12/schema";
    private static final String TITLE = "LazyclaudeProtocol";
    private static final String DESCRIPTION =
            "Every method and notification of the lazyclaude sidecar, generated from its Payloads and Views.";
    private static final String SCHEMA = "$schema";
    private static final String TITLE_KEY = "title";
    private static final String DESCRIPTION_KEY = "description";
    private static final String DEFS = "$defs";
    private static final String METHODS = "methods";
    private static final String INPUTS = "inputs";
    private static final String NOTIFICATIONS = "notifications";
    private static final String PARAMS = "params";
    private static final String RESULT = "result";

    private final SchemaDefinitions definitions = new SchemaDefinitions();
    private final TypeSchema types = new TypeSchema(definitions);

    private ProtocolSchema() {}

    static String text(Protocol protocol) {
        return SidecarJson.pretty(new ProtocolSchema().root(protocol));
    }

    private ObjectNode root(Protocol protocol) {
        ObjectNode methods = SidecarJson.object();
        protocol.methods().forEach(method -> methods.set(method.name(), call(method)));
        ObjectNode inputs = SidecarJson.object();
        protocol.inputs().forEach(input -> inputs.set(input.name(), taken(input)));
        ObjectNode notifications = SidecarJson.object();
        notifications.set(JsonRpcMessages.readyName(), ready());
        protocol.notifications().forEach(notification -> notifications.set(notification.name(), push(notification)));
        ObjectNode properties = SidecarJson.object();
        properties.set(METHODS, SchemaNodes.closedObject(methods));
        properties.set(INPUTS, SchemaNodes.closedObject(inputs));
        properties.set(NOTIFICATIONS, SchemaNodes.closedObject(notifications));
        ObjectNode root = SidecarJson.object();
        root.put(SCHEMA, SCHEMA_URI);
        root.put(TITLE_KEY, TITLE);
        root.put(DESCRIPTION_KEY, DESCRIPTION);
        root.setAll(SchemaNodes.closedObject(properties));
        root.set(DEFS, definitions.all());
        return root;
    }

    private ObjectNode call(ProtocolMethod<?, ?> method) {
        ObjectNode properties = SidecarJson.object();
        properties.set(PARAMS, types.read(method.params()));
        properties.set(RESULT, types.written(method.result()));
        return SchemaNodes.closedObject(properties);
    }

    private ObjectNode taken(ProtocolInput<?> input) {
        return params(types.read(input.params()));
    }

    private ObjectNode push(ProtocolNotification<?> notification) {
        return params(types.written(notification.params()));
    }

    private static ObjectNode ready() {
        ObjectNode properties = SidecarJson.object();
        properties.set(JsonRpcMessages.protocolField(), SchemaNodes.constant(JsonRpcMessages.protocolVersion()));
        return params(SchemaNodes.closedObject(properties));
    }

    private static ObjectNode params(JsonNode schema) {
        ObjectNode properties = SidecarJson.object();
        properties.set(PARAMS, schema);
        return SchemaNodes.closedObject(properties);
    }
}

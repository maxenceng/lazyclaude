package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/** The messages the sidecar writes. */
final class JsonRpcMessages {

    private static final String JSONRPC = "jsonrpc";
    private static final String VERSION = "2.0";
    private static final String ID = "id";
    private static final String METHOD = "method";
    private static final String PARAMS = "params";
    private static final String RESULT = "result";
    private static final String ERROR = "error";
    private static final String CODE = "code";
    private static final String MESSAGE = "message";
    private static final String READY = "ready";
    private static final String PROTOCOL = "protocol";
    private static final int PROTOCOL_VERSION = 1;

    private JsonRpcMessages() {}

    static JsonNode ready() {
        ObjectNode params = SidecarJson.object();
        params.put(PROTOCOL, PROTOCOL_VERSION);
        return notification(READY, params);
    }

    static JsonNode notification(String method, JsonNode params) {
        ObjectNode message = envelope();
        message.put(METHOD, method);
        message.set(PARAMS, params);
        return message;
    }

    static JsonNode result(JsonNode id, JsonNode result) {
        ObjectNode message = envelope();
        message.set(ID, id);
        message.set(RESULT, result);
        return message;
    }

    /** A fault answers a request by its {@code id}; one for a line that could not be read has none to echo, so it is null. */
    static JsonNode fault(JsonNode id, ErrorCode code, String text) {
        ObjectNode error = SidecarJson.object();
        error.put(CODE, code.code());
        error.put(MESSAGE, text);
        ObjectNode message = envelope();
        message.set(ID, id);
        message.set(ERROR, error);
        return message;
    }

    static JsonNode unidentified() {
        return NullNode.getInstance();
    }

    private static ObjectNode envelope() {
        ObjectNode message = SidecarJson.object();
        message.put(JSONRPC, VERSION);
        return message;
    }
}

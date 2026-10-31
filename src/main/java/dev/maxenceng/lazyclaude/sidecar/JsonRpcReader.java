package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/** Reads a line into a call. */
final class JsonRpcReader {

    private static final String ID = "id";
    private static final String METHOD = "method";
    private static final String PARAMS = "params";
    private static final String NOT_JSON = "not JSON: %s";

    private JsonRpcReader() {}

    /** @throws JsonRpcFaultException when the line is not JSON, or is JSON that is not a call */
    static JsonRpcCall read(String line) {
        JsonNode message = parsed(line);
        if (!message.isObject() || !message.path(METHOD).isString()) {
            throw new JsonRpcFaultException(ErrorCode.INVALID_REQUEST, "not a JSON-RPC call: it has no method");
        }
        String method = message.path(METHOD).asString();
        JsonNode params = message.path(PARAMS);
        return message.has(ID) ? new JsonRpcRequest(message.get(ID), method, params) : new JsonRpcNotification(method, params);
    }

    private static JsonNode parsed(String line) {
        try {
            return SidecarJson.parse(line);
        } catch (JacksonException e) {
            throw new JsonRpcFaultException(ErrorCode.PARSE_ERROR, NOT_JSON.formatted(e.getOriginalMessage()), e);
        }
    }
}

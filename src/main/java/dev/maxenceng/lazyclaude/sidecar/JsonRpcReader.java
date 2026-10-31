package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/** Reads a line into a call. */
final class JsonRpcReader {

    private static final String ID = "id";
    private static final String METHOD = "method";
    private static final String PARAMS = "params";
    private static final String NOT_JSON = "not JSON: %s";
    private static final String JSONRPC = "jsonrpc";
    private static final String VERSION = "2.0";
    private static final String NO_METHOD = "not a JSON-RPC call: it has no method";
    private static final String NOT_VERSION_2 = "not a JSON-RPC 2.0 call: its jsonrpc is not \"2.0\"";

    private JsonRpcReader() {}

    /** @throws JsonRpcFaultException when the line is not JSON, or is JSON that is not a JSON-RPC 2.0 call */
    // deferred: a batch, an array of calls, is refused as one invalid request — close it when a frontend sends one.
    static JsonRpcCall read(String line) {
        JsonNode message = parsed(line);
        if (!message.isObject() || !message.path(METHOD).isString()) {
            throw new JsonRpcFaultException(ErrorCode.INVALID_REQUEST, NO_METHOD);
        }
        JsonNode version = message.path(JSONRPC);
        if (!version.isString() || !VERSION.equals(version.asString())) {
            throw new JsonRpcFaultException(ErrorCode.INVALID_REQUEST, NOT_VERSION_2);
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

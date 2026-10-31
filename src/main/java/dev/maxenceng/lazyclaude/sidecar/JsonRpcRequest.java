package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;

/** A call that carries an {@code id}, which its response echoes. */
record JsonRpcRequest(JsonNode id, String method, JsonNode params) implements JsonRpcCall {}

package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;

/** A call without an {@code id}: nothing answers it, not even a fault. */
record JsonRpcNotification(String method, JsonNode params) implements JsonRpcCall {}

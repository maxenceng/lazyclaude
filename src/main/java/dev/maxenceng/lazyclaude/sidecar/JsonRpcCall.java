package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;

/** A line the frontend sent: a request, which is answered, or a notification, which is not. */
sealed interface JsonRpcCall permits JsonRpcRequest, JsonRpcNotification {

    String method();

    JsonNode params();
}

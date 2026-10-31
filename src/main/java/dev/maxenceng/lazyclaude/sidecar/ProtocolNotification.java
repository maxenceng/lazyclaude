package dev.maxenceng.lazyclaude.sidecar;

import tools.jackson.databind.JsonNode;

/** A notification the sidecar pushes, as the schema states it: its name and the View type its params are, the only type it is sent with. */
record ProtocolNotification<V>(String name, Class<V> params) {

    JsonNode message(V view) {
        return JsonRpcMessages.notification(name, SidecarJson.tree(view));
    }
}

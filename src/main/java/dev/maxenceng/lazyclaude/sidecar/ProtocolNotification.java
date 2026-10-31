package dev.maxenceng.lazyclaude.sidecar;

/** A notification the sidecar pushes, as the schema states it: its name and the View type its params are. */
record ProtocolNotification(String name, Class<?> params) {}

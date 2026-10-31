package dev.maxenceng.lazyclaude.sidecar;

/** A protocol method as the schema states it: the Payload its params are read into and the View type it answers with (ADR 0050). */
record ProtocolMethod<P>(String name, Class<P> params, Class<?> result) {}

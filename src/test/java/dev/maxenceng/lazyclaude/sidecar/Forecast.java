package dev.maxenceng.lazyclaude.sidecar;

public sealed interface Forecast permits Drizzle, Storm {}

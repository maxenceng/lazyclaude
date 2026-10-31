package dev.maxenceng.lazyclaude.sidecar;

public record WrappedSealedPayload(SealedPayload inner, String label) {}

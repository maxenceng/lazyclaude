package dev.maxenceng.lazyclaude.sidecar;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;

public record NestedPayload(BoardPayload inner, String label) {}

package dev.maxenceng.lazyclaude.sidecar;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardLookupView;

public record SealedPayload(BoardLookupView inner, String label) {}

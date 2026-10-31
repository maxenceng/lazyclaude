package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;

public record ListPayload(List<BoardPayload> inner, String label) {}

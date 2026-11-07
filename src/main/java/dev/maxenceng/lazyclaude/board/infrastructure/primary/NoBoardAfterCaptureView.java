package dev.maxenceng.lazyclaude.board.infrastructure.primary;

/** The board could not be read once the ticket was captured. */
public record NoBoardAfterCaptureView() implements BoardAfterCaptureView {}

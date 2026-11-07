package dev.maxenceng.lazyclaude.board.infrastructure.primary;

/** The board read once a ticket was captured, or nothing when that read failed: the frontend then keeps the board it shows. */
public sealed interface BoardAfterCaptureView permits BoardReadingView, NoBoardAfterCaptureView {}

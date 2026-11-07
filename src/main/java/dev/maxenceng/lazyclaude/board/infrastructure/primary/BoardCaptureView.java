package dev.maxenceng.lazyclaude.board.infrastructure.primary;

/** What capturing a ticket answers a frontend: the ticket GitHub numbered and the board read just after, or why it was not written. */
public sealed interface BoardCaptureView permits CapturedOnBoardView, RefusedTicketCaptureView {}

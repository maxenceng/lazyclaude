package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.published.TicketId;

/** GitHub took the ticket and numbered it; the board is read again to show it. */
public record CapturedTicketView(TicketId id) implements TicketCaptureView {}

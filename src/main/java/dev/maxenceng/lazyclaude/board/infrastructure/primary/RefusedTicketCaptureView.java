package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.published.Reason;

/** The ticket was not written, or not known to be, and why; trying again is the person's call. */
public record RefusedTicketCaptureView(Reason reason) implements TicketCaptureView {}

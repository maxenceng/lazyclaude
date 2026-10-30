package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;

public final class TicketCaptureFixture {

    private TicketCaptureFixture() {}

    public static TicketCapture capturedTicket() {
        return new CapturedTicket(ticketId());
    }

    public static TicketCapture refusedCapture() {
        return new RefusedTicketCapture(reason());
    }

    public static TicketCaptureRefusedException captureRefused() {
        return new TicketCaptureRefusedException(reason().value());
    }
}

package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.CapturedTicket;
import dev.maxenceng.lazyclaude.board.domain.RefusedTicketCapture;
import dev.maxenceng.lazyclaude.board.domain.TicketCapture;

/** What capturing a ticket came to: GitHub took it and numbered it, or it did not. */
public sealed interface TicketCaptureView permits CapturedTicketView, RefusedTicketCaptureView {

    static TicketCaptureView from(TicketCapture capture) {
        return switch (capture) {
            case CapturedTicket captured -> new CapturedTicketView(captured.id());
            case RefusedTicketCapture refused -> new RefusedTicketCaptureView(refused.reason());
        };
    }
}

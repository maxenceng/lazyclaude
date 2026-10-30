package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketIdNumbered;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;

public final class TicketCaptureViewFixture {

    private TicketCaptureViewFixture() {}

    public static TicketCapturePayload capturePayload() {
        return new TicketCapturePayload(project().root(), title().value());
    }

    public static TicketCapturePayload blankTitlePayload() {
        return new TicketCapturePayload(project().root(), "   ");
    }

    public static TicketCaptureView capturedTicketViewNumbered(int number) {
        return new CapturedTicketView(ticketIdNumbered(number));
    }

    public static TicketCaptureView capturedTicketView() {
        return new CapturedTicketView(ticketId());
    }

    public static TicketCaptureView refusedTicketCaptureView() {
        return new RefusedTicketCaptureView(reason());
    }
}

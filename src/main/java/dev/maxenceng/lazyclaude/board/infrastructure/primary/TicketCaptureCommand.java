package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.application.TicketCaptureApplicationService;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.error.domain.AssertionException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * What the shell asks of {@code board} to capture a draft ticket as a new GitHub issue (ADR 0054). It waits for {@code gh}, so the
 * caller is off the render thread, and answers what GitHub took or why it did not; it never fails, and never retries.
 */
public final class TicketCaptureCommand {

    private final TicketCaptureApplicationService tickets;

    public TicketCaptureCommand(TicketCaptureApplicationService tickets) {
        this.tickets = tickets;
    }

    /** A blank title is refused before anything is asked of GitHub. */
    public TicketCaptureView capture(TicketCapturePayload payload) {
        try {
            return TicketCaptureView.from(tickets.capture(new Project(payload.root()), new Title(payload.title())));
        } catch (AssertionException invalid) {
            return new RefusedTicketCaptureView(new Reason(invalid.getMessage()));
        }
    }
}

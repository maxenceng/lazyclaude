package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Captures a new ticket as an issue with no labels, starting from the project's template. */
public final class TicketCaptureManager {

    private final TicketCapturePort captures;
    private final TicketTemplatePort templates;

    public TicketCaptureManager(TicketCapturePort captures, TicketTemplatePort templates) {
        this.captures = captures;
        this.templates = templates;
    }

    /** What GitHub answered; a failure is a refusal that says why, never an exception. */
    public TicketCapture capture(Project project, Title title) {
        try {
            return new CapturedTicket(captures.create(project, title, templates.templateOf(project).sections()));
        } catch (TicketCaptureRefusedException refused) {
            return new RefusedTicketCapture(new Reason(refused.getMessage()));
        }
    }
}

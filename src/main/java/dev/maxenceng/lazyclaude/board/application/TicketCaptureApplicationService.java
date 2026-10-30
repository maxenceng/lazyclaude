package dev.maxenceng.lazyclaude.board.application;

import dev.maxenceng.lazyclaude.board.domain.TicketCapture;
import dev.maxenceng.lazyclaude.board.domain.TicketCaptureManager;
import dev.maxenceng.lazyclaude.board.domain.TicketCapturePort;
import dev.maxenceng.lazyclaude.board.domain.TicketTemplatePort;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Captures a new ticket on the project's GitHub remote, when asked. */
public final class TicketCaptureApplicationService {

    private final TicketCaptureManager manager;

    public TicketCaptureApplicationService(TicketCapturePort captures, TicketTemplatePort templates) {
        this.manager = new TicketCaptureManager(captures, templates);
    }

    public TicketCapture capture(Project project, Title title) {
        return manager.capture(project, title);
    }
}

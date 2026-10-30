package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a project keeps the template a new ticket starts from. */
public interface TicketTemplatePort {

    /**
     * The project's ticket template, or none when it has none.
     *
     * @throws TicketCaptureRefusedException when the template exists but cannot be read
     */
    TicketTemplate templateOf(Project project);
}

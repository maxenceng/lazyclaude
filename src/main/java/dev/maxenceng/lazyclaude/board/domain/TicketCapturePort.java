package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a new ticket is written: the issues of the project's GitHub remote. */
public interface TicketCapturePort {

    /**
     * Creates an issue with {@code title}, no labels and {@code body}, and answers the number GitHub gave it. Nothing is retried: a
     * create that timed out may have succeeded.
     *
     * @throws TicketCaptureRefusedException when the project has no GitHub remote, GitHub cannot be reached or refuses it, or
     *     {@code gh} cannot be used
     */
    TicketId create(Project project, Title title, TicketBody body);
}

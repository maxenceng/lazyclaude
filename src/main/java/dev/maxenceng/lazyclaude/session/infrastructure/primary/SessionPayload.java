package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.KnownTickets;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * What {@link SessionQuery} is asked with: the root of the project whose sessions are wanted, and the ids of the tickets on its
 * board, which the shell hands over so that {@code session} never reads {@code board}'s types.
 */
public record SessionPayload(Path root, List<LinkedTicketId> ticketIds) {

    public SessionPayload {
        Assert.notNull("ticket ids", ticketIds);
    }

    KnownTickets tickets() {
        return new KnownTickets(ticketIds);
    }

    Project toDomain() {
        return new Project(root);
    }
}

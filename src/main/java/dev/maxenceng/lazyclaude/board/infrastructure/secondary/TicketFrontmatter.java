package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketTitle;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.board.published.TypeName;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The four frontmatter fields the board reads, as text; any of them is null where the file lacks it. */
record TicketFrontmatter(String id, String status, String context, String type) {

    boolean claimsATicket() {
        return id != null;
    }

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when a field is refused, saying which */
    Ticket toDomain(TicketTitle title) {
        Assert.notNull("status", status);
        return Ticket.builder()
                .id(new TicketId(id))
                .title(title)
                .status(Status.parse(status))
                .context(ticketContext())
                .type(ticketType())
                .build();
    }

    private TicketContext ticketContext() {
        return isAbsent(context) ? new NoContext() : new ContextName(context);
    }

    private TicketType ticketType() {
        return isAbsent(type) ? new NoType() : new TypeName(type);
    }

    private static boolean isAbsent(String field) {
        return field == null || field.isBlank();
    }
}

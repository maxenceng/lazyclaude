package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.FrontmatterFixture.frontmatter;
import static dev.maxenceng.lazyclaude.board.published.BodyFixture.body;
import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;

import dev.maxenceng.lazyclaude.board.domain.Ticket.TicketBuilder;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;

public final class TicketFixture {

    private TicketFixture() {}

    public static TicketBuilder ticketBuilder() {
        return Ticket.builder()
                .id(ticketId())
                .status(Status.TODO)
                .context(contextName())
                .type(typeName())
                .frontmatter(frontmatter())
                .body(body());
    }

    public static Ticket ticket() {
        return ticketBuilder().build();
    }

    public static Ticket ticketWithBody(String text) {
        return ticketBuilder().body(TicketBody.of(text)).build();
    }

    public static Ticket bareTicket() {
        return bareTicketBuilder().build();
    }

    public static TicketBuilder bareTicketBuilder() {
        return ticketBuilder()
                .context(new NoContext())
                .type(new NoType())
                .body(new NoBody());
    }
}

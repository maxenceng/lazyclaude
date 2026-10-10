package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;

import dev.maxenceng.lazyclaude.board.domain.Ticket.TicketBuilder;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;

public final class TicketFixture {

    private TicketFixture() {}

    public static TicketBuilder ticketBuilder() {
        return Ticket.builder()
                .id(ticketId())
                .title(title())
                .status(Status.TODO)
                .context(contextName())
                .type(typeName());
    }

    public static Ticket ticket() {
        return ticketBuilder().build();
    }

    public static Ticket bareTicket() {
        return ticketBuilder()
                .title(new NoTitle())
                .context(new NoContext())
                .type(new NoType())
                .build();
    }
}

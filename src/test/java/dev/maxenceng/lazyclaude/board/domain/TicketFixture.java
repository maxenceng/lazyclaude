package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.published.BodyFixture.body;
import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.Ticket.TicketBuilder;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;

public final class TicketFixture {

    private TicketFixture() {}

    public static TicketBuilder ticketBuilder() {
        return Ticket.builder()
                .id(ticketId())
                .branch(ticketBranch())
                .status(Status.TODO)
                .context(contextName())
                .type(typeName())
                .title(title())
                .otherLabels(List.of())
                .body(body());
    }

    public static Ticket ticket() {
        return ticketBuilder().build();
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

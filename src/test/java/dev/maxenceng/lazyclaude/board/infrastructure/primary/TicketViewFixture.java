package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.secondTicketId;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.secondTitle;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView.TicketViewBuilder;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;

public final class TicketViewFixture {

    private TicketViewFixture() {}

    public static TicketViewBuilder ticketViewBuilder() {
        return TicketView.builder()
                .id(ticketId())
                .title(title())
                .status(Status.DRAFT)
                .context(contextName())
                .type(typeName());
    }

    public static TicketView ticketView() {
        return ticketViewBuilder().build();
    }

    public static TicketView secondTicketView() {
        return ticketViewBuilder().id(secondTicketId()).title(secondTitle()).build();
    }

    public static TicketView doneTicketView() {
        return ticketViewBuilder().status(Status.DONE).build();
    }

    public static TicketView untitledTicketView() {
        return ticketViewBuilder().title(new NoTitle()).build();
    }

    public static TicketView bareTicketView() {
        return ticketViewBuilder().context(new NoContext()).type(new NoType()).build();
    }
}

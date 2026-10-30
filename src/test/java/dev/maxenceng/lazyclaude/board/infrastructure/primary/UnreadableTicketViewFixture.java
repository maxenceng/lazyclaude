package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.secondTicketId;

public final class UnreadableTicketViewFixture {

    private UnreadableTicketViewFixture() {}

    public static UnreadableTicketView unreadableTicketView() {
        return UnreadableTicketView.builder().id(secondTicketId()).reason(reason()).build();
    }
}

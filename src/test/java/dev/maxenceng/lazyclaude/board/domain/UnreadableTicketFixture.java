package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.secondFileName;
import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.reason;

import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket.UnreadableTicketBuilder;

public final class UnreadableTicketFixture {

    private UnreadableTicketFixture() {}

    public static UnreadableTicketBuilder unreadableTicketBuilder() {
        return UnreadableTicket.builder().file(secondFileName()).reason(reason());
    }

    public static UnreadableTicket unreadableTicket() {
        return unreadableTicketBuilder().build();
    }
}

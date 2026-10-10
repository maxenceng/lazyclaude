package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.secondFileName;
import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.reason;

public final class UnreadableTicketViewFixture {

    private UnreadableTicketViewFixture() {}

    public static UnreadableTicketView unreadableTicketView() {
        return UnreadableTicketView.builder().file(secondFileName()).reason(reason()).build();
    }
}

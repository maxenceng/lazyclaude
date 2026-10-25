package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;

final class StatusColumnsFixture {

    private StatusColumnsFixture() {}

    static StatusColumns columnsOf(TicketView... tickets) {
        return new StatusColumns(boardViewOf(tickets));
    }
}

package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.Board;

/** A board as the shell reads it: one group per status in lifecycle order, then the unreadable files. */
public record BoardView(List<StatusGroupView> groups, List<UnreadableTicketView> unreadable) {

    static BoardView from(Board board) {
        return new BoardView(
                board.groups().stream().map(StatusGroupView::from).toList(),
                board.unreadable().stream().map(UnreadableTicketView::from).toList());
    }
}

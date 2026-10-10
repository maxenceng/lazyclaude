package dev.maxenceng.lazyclaude.board.published;

import java.util.List;

/** A board as another module reads it: one group per status in lifecycle order, then the unreadable files. */
public record BoardView(List<StatusGroupView> groups, List<UnreadableTicketView> unreadable) {

    public BoardView {
        groups = List.copyOf(groups);
        unreadable = List.copyOf(unreadable);
    }
}

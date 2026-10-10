package dev.maxenceng.lazyclaude.board.published;

import java.util.List;

/** The tickets of one status, in id order; {@code finished} marks the end of the lifecycle. */
public record StatusGroupView(String status, boolean finished, List<TicketView> tickets) {

    public StatusGroupView {
        tickets = List.copyOf(tickets);
    }
}

package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.StatusGroup;
import dev.maxenceng.lazyclaude.board.published.Status;

/** The tickets of one status, in id order. */
public record StatusGroupView(Status status, List<TicketView> tickets) {

    static StatusGroupView from(StatusGroup group) {
        return new StatusGroupView(group.status(), group.tickets().stream().map(TicketView::from).toList());
    }
}

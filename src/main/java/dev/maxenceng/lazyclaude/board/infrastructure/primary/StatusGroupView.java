package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.StatusGroup;
import dev.maxenceng.lazyclaude.board.published.Status;

/** The tickets of one status, in id order. */
public record StatusGroupView(Status status, List<TicketView> tickets) {

    static StatusGroupView from(StatusGroup group) {
        return builder()
                .status(group.status())
                .tickets(group.tickets().stream().map(TicketView::from).toList())
                .build();
    }

    public static StatusGroupViewBuilder builder() {
        return new StatusGroupViewBuilder();
    }

    public static final class StatusGroupViewBuilder {
        private Status status;
        private List<TicketView> tickets;

        private StatusGroupViewBuilder() {}

        public StatusGroupViewBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public StatusGroupViewBuilder tickets(List<TicketView> tickets) {
            this.tickets = tickets;
            return this;
        }

        public StatusGroupView build() {
            return new StatusGroupView(status, tickets);
        }
    }
}

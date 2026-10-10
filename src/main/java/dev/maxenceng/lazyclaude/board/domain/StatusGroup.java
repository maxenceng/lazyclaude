package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The tickets of one status, in id order. */
public record StatusGroup(Status status, List<Ticket> tickets) {

    public StatusGroup {
        Assert.notNull("status", status);
        Assert.notNull("tickets", tickets);
    }

    public static StatusGroupBuilder builder() {
        return new StatusGroupBuilder();
    }

    public static final class StatusGroupBuilder {
        private Status status;
        private List<Ticket> tickets;

        private StatusGroupBuilder() {}

        public StatusGroupBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public StatusGroupBuilder tickets(List<Ticket> tickets) {
            this.tickets = tickets;
            return this;
        }

        public StatusGroup build() {
            return new StatusGroup(status, tickets);
        }
    }
}

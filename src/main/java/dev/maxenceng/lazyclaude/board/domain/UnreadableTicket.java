package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket whose labels or state the board refused to read. */
public record UnreadableTicket(TicketId id, Reason reason) implements BacklogEntry {

    public UnreadableTicket {
        Assert.notNull("id", id);
        Assert.notNull("reason", reason);
    }

    public static UnreadableTicketBuilder builder() {
        return new UnreadableTicketBuilder();
    }

    public static final class UnreadableTicketBuilder {
        private TicketId id;
        private Reason reason;

        private UnreadableTicketBuilder() {}

        public UnreadableTicketBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public UnreadableTicketBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnreadableTicket build() {
            return new UnreadableTicket(id, reason);
        }
    }
}

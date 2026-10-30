package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.TicketId;

/** An issue that could not be read as a ticket, and why. */
public record UnreadableTicketView(TicketId id, Reason reason) {

    static UnreadableTicketView from(UnreadableTicket unreadable) {
        return builder().id(unreadable.id()).reason(unreadable.reason()).build();
    }

    public static UnreadableTicketViewBuilder builder() {
        return new UnreadableTicketViewBuilder();
    }

    public static final class UnreadableTicketViewBuilder {
        private TicketId id;
        private Reason reason;

        private UnreadableTicketViewBuilder() {}

        public UnreadableTicketViewBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public UnreadableTicketViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnreadableTicketView build() {
            return new UnreadableTicketView(id, reason);
        }
    }
}

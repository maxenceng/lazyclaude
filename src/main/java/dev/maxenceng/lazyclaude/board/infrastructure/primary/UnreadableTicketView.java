package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Reason;

/** A backlog file that could not be read as a ticket, and why. */
public record UnreadableTicketView(FileName file, Reason reason) {

    static UnreadableTicketView from(UnreadableTicket unreadable) {
        return builder().file(unreadable.file()).reason(unreadable.reason()).build();
    }

    public static UnreadableTicketViewBuilder builder() {
        return new UnreadableTicketViewBuilder();
    }

    public static final class UnreadableTicketViewBuilder {
        private FileName file;
        private Reason reason;

        private UnreadableTicketViewBuilder() {}

        public UnreadableTicketViewBuilder file(FileName file) {
            this.file = file;
            return this;
        }

        public UnreadableTicketViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnreadableTicketView build() {
            return new UnreadableTicketView(file, reason);
        }
    }
}

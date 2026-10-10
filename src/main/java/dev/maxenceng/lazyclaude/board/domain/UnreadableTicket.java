package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A backlog file with an `id` whose frontmatter or status was refused, or whose `id` was already claimed. */
public record UnreadableTicket(FileName file, Reason reason) implements BacklogEntry {

    public UnreadableTicket {
        Assert.notNull("file", file);
        Assert.notNull("reason", reason);
    }

    public static UnreadableTicketBuilder builder() {
        return new UnreadableTicketBuilder();
    }

    public static final class UnreadableTicketBuilder {
        private FileName file;
        private Reason reason;

        private UnreadableTicketBuilder() {}

        public UnreadableTicketBuilder file(FileName file) {
            this.file = file;
            return this;
        }

        public UnreadableTicketBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnreadableTicket build() {
            return new UnreadableTicket(file, reason);
        }
    }
}

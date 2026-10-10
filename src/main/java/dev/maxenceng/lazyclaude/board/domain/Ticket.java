package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketTitle;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One backlog ticket as the board shows it; a field the file lacks is an explicit absence, never a stand-in. */
public record Ticket(TicketId id, TicketTitle title, Status status, TicketContext context, TicketType type) {

    public Ticket {
        Assert.notNull("id", id);
        Assert.notNull("title", title);
        Assert.notNull("status", status);
        Assert.notNull("context", context);
        Assert.notNull("type", type);
    }

    public static TicketBuilder builder() {
        return new TicketBuilder();
    }

    public static final class TicketBuilder {
        private TicketId id;
        private TicketTitle title;
        private Status status;
        private TicketContext context;
        private TicketType type;

        private TicketBuilder() {}

        public TicketBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public TicketBuilder title(TicketTitle title) {
            this.title = title;
            return this;
        }

        public TicketBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public TicketBuilder context(TicketContext context) {
            this.context = context;
            return this;
        }

        public TicketBuilder type(TicketType type) {
            this.type = type;
            return this;
        }

        public Ticket build() {
            return new Ticket(id, title, status, context, type);
        }
    }
}

package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The session ran {@code /ticket} on the ticket, the last time with {@code verb}. */
public record StrongLink(LinkedTicketId ticket, LinkVerb verb) implements TicketLink {

    public StrongLink {
        Assert.notNull("ticket", ticket);
        Assert.notNull("verb", verb);
    }

    public static StrongLinkBuilder builder() {
        return new StrongLinkBuilder();
    }

    public static final class StrongLinkBuilder {
        private LinkedTicketId ticket;
        private LinkVerb verb;

        private StrongLinkBuilder() {}

        public StrongLinkBuilder ticket(LinkedTicketId ticket) {
            this.ticket = ticket;
            return this;
        }

        public StrongLinkBuilder verb(LinkVerb verb) {
            this.verb = verb;
            return this;
        }

        public StrongLink build() {
            return new StrongLink(ticket, verb);
        }
    }
}

package dev.maxenceng.lazyclaude.session.published;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The session ran {@code /ticket} on the ticket, the last time with {@code verb}. */
public record TicketLink(LinkedTicketId ticket, LinkVerb verb) {

    public TicketLink {
        Assert.notNull("ticket", ticket);
        Assert.notNull("verb", verb);
    }

    /** The link among {@code links} to the ticket spelt {@code ticket}, if there is one. */
    public static Optional<TicketLink> to(List<TicketLink> links, LinkedTicketId ticket) {
        return links.stream().filter(link -> link.ticket().equals(ticket)).findFirst();
    }

    public static TicketLinkBuilder builder() {
        return new TicketLinkBuilder();
    }

    public static final class TicketLinkBuilder {
        private LinkedTicketId ticket;
        private LinkVerb verb;

        private TicketLinkBuilder() {}

        public TicketLinkBuilder ticket(LinkedTicketId ticket) {
            this.ticket = ticket;
            return this;
        }

        public TicketLinkBuilder verb(LinkVerb verb) {
            this.verb = verb;
            return this;
        }

        public TicketLink build() {
            return new TicketLink(ticket, verb);
        }
    }
}

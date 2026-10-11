package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;

/** One {@code /ticket <verb> <ID…>} a session ran, as its transcript spelt it; a run names at least one id. */
public record TicketRun(LinkVerb verb, List<NamedTicketId> ids) {

    public TicketRun {
        Assert.notNull("verb", verb);
        Assert.notEmpty("ids", ids);
    }

    public static TicketRunBuilder builder() {
        return new TicketRunBuilder();
    }

    public static final class TicketRunBuilder {
        private LinkVerb verb;
        private List<NamedTicketId> ids;

        private TicketRunBuilder() {}

        public TicketRunBuilder verb(LinkVerb verb) {
            this.verb = verb;
            return this;
        }

        public TicketRunBuilder ids(List<NamedTicketId> ids) {
            this.ids = ids;
            return this;
        }

        public TicketRun build() {
            return new TicketRun(verb, ids);
        }
    }
}

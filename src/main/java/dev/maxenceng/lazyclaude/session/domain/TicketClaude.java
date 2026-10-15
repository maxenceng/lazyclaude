package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** {@code claude -n "<ID> <verb>" "/ticket <verb> <ID>"}: a session begun to run a verb on a ticket. */
public record TicketClaude(LinkedTicketId ticket, Verb verb) implements LaunchCommand {

    public TicketClaude {
        Assert.notNull("ticket", ticket);
        Assert.notNull("verb", verb);
    }

    public static TicketClaudeBuilder builder() {
        return new TicketClaudeBuilder();
    }

    public static final class TicketClaudeBuilder {
        private LinkedTicketId ticket;
        private Verb verb;

        private TicketClaudeBuilder() {}

        public TicketClaudeBuilder ticket(LinkedTicketId ticket) {
            this.ticket = ticket;
            return this;
        }

        public TicketClaudeBuilder verb(Verb verb) {
            this.verb = verb;
            return this;
        }

        public TicketClaude build() {
            return new TicketClaude(ticket, verb);
        }
    }
}

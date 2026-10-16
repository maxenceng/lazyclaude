package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** What starting a session from a ticket is asked with: the ticket, its worktree, and the verb to run on it. */
public record TicketStart(TicketWorktree worktree, LinkedTicketId ticket, Verb verb) {

    public TicketStart {
        Assert.notNull("worktree", worktree);
        Assert.notNull("ticket", ticket);
        Assert.notNull("verb", verb);
    }

    public static TicketStartBuilder builder() {
        return new TicketStartBuilder();
    }

    public static final class TicketStartBuilder {
        private TicketWorktree worktree;
        private LinkedTicketId ticket;
        private Verb verb;

        private TicketStartBuilder() {}

        public TicketStartBuilder worktree(TicketWorktree worktree) {
            this.worktree = worktree;
            return this;
        }

        public TicketStartBuilder ticket(LinkedTicketId ticket) {
            this.ticket = ticket;
            return this;
        }

        public TicketStartBuilder verb(Verb verb) {
            this.verb = verb;
            return this;
        }

        public TicketStart build() {
            return new TicketStart(worktree, ticket, verb);
        }
    }
}

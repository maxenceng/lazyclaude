package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionFacts;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** A session with no live process, only a transcript: what that transcript says of it or why it cannot be read, and the tickets it links the session to. */
public record PastSession(
        SessionId id,
        SessionFacts facts,
        WorkingDirectory directory,
        LastActivity lastActivity,
        StartedAt startedAt,
        List<TicketLink> links) {

    public PastSession {
        Assert.notNull("id", id);
        Assert.notNull("facts", facts);
        Assert.notNull("directory", directory);
        Assert.notNull("last activity", lastActivity);
        Assert.notNull("started at", startedAt);
        Assert.notNull("links", links);
    }

    public PastSession linkedTo(List<TicketLink> links) {
        return new PastSession(id, facts, directory, lastActivity, startedAt, links);
    }

    public static PastSessionBuilder builder() {
        return new PastSessionBuilder();
    }

    public static final class PastSessionBuilder {
        private SessionId id;
        private SessionFacts facts;
        private WorkingDirectory directory;
        private LastActivity lastActivity;
        private StartedAt startedAt;
        private List<TicketLink> links = List.of();

        private PastSessionBuilder() {}

        public PastSessionBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public PastSessionBuilder facts(SessionFacts facts) {
            this.facts = facts;
            return this;
        }

        public PastSessionBuilder directory(WorkingDirectory directory) {
            this.directory = directory;
            return this;
        }

        public PastSessionBuilder lastActivity(LastActivity lastActivity) {
            this.lastActivity = lastActivity;
            return this;
        }

        public PastSessionBuilder startedAt(StartedAt startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public PastSessionBuilder links(List<TicketLink> links) {
            this.links = links;
            return this;
        }

        public PastSession build() {
            return new PastSession(id, facts, directory, lastActivity, startedAt, links);
        }
    }
}

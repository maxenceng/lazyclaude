package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.OpeningPrompt;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.TranscriptReading;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * A launch-log entry with no live process: what its transcript last said of it, whether it can be read now, the tickets it links
 * the session to, and whether lazyclaude's quit, a crash or a kill cut it off rather than its {@code claude} exiting by itself.
 */
public record PastSession(
        SessionId id,
        SessionTitle title,
        OpeningPrompt opening,
        TranscriptReading reading,
        WorkingDirectory directory,
        LastActivity lastActivity,
        StartedAt startedAt,
        List<TicketLink> links,
        boolean interrupted) {

    public PastSession {
        Assert.notNull("id", id);
        Assert.notNull("title", title);
        Assert.notNull("opening prompt", opening);
        Assert.notNull("reading", reading);
        Assert.notNull("directory", directory);
        Assert.notNull("last activity", lastActivity);
        Assert.notNull("started at", startedAt);
        Assert.notNull("links", links);
    }

    public PastSession linkedTo(List<TicketLink> links) {
        return new PastSession(id, title, opening, reading, directory, lastActivity, startedAt, links, interrupted);
    }

    public static PastSessionBuilder builder() {
        return new PastSessionBuilder();
    }

    public static final class PastSessionBuilder {
        private SessionId id;
        private SessionTitle title;
        private OpeningPrompt opening;
        private TranscriptReading reading;
        private WorkingDirectory directory;
        private LastActivity lastActivity;
        private StartedAt startedAt;
        private List<TicketLink> links = List.of();
        private boolean interrupted;

        private PastSessionBuilder() {}

        public PastSessionBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public PastSessionBuilder title(SessionTitle title) {
            this.title = title;
            return this;
        }

        public PastSessionBuilder opening(OpeningPrompt opening) {
            this.opening = opening;
            return this;
        }

        public PastSessionBuilder reading(TranscriptReading reading) {
            this.reading = reading;
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

        public PastSessionBuilder interrupted(boolean interrupted) {
            this.interrupted = interrupted;
            return this;
        }

        public PastSession build() {
            return new PastSession(id, title, opening, reading, directory, lastActivity, startedAt, links, interrupted);
        }
    }
}

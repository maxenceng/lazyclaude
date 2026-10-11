package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** A session with no live process, only a transcript: what that transcript says of it. */
public record PastSession(
        SessionId id,
        SessionTitle title,
        WorkingDirectory directory,
        LastActivity lastActivity,
        StartedAt startedAt) {

    public PastSession {
        Assert.notNull("id", id);
        Assert.notNull("title", title);
        Assert.notNull("directory", directory);
        Assert.notNull("last activity", lastActivity);
        Assert.notNull("started at", startedAt);
    }

    public static PastSessionBuilder builder() {
        return new PastSessionBuilder();
    }

    public static final class PastSessionBuilder {
        private SessionId id;
        private SessionTitle title;
        private WorkingDirectory directory;
        private LastActivity lastActivity;
        private StartedAt startedAt;

        private PastSessionBuilder() {}

        public PastSessionBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public PastSessionBuilder title(SessionTitle title) {
            this.title = title;
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

        public PastSession build() {
            return new PastSession(id, title, directory, lastActivity, startedAt);
        }
    }
}

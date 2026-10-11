package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** A past session as the cache holds it, with where its {@code cwd} was found to belong. */
public record LocatedPastSession(PastSession session, ProjectLocation location) {

    public LocatedPastSession {
        Assert.notNull("session", session);
        Assert.notNull("location", location);
    }

    boolean belongsTo(Project project) {
        return location.holds(project, session.directory());
    }

    public static LocatedPastSessionBuilder builder() {
        return new LocatedPastSessionBuilder();
    }

    public static final class LocatedPastSessionBuilder {
        private PastSession session;
        private ProjectLocation location;

        private LocatedPastSessionBuilder() {}

        public LocatedPastSessionBuilder session(PastSession session) {
            this.session = session;
            return this;
        }

        public LocatedPastSessionBuilder location(ProjectLocation location) {
            this.location = location;
            return this;
        }

        public LocatedPastSession build() {
            return new LocatedPastSession(session, location);
        }
    }
}

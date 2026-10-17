package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A live session with the place it belongs to. */
public record PlacedSession(SessionPlace place, LiveSession session) {

    public PlacedSession {
        Assert.notNull("place", place);
        Assert.notNull("session", session);
    }

    public static PlacedSessionBuilder builder() {
        return new PlacedSessionBuilder();
    }

    public static final class PlacedSessionBuilder {
        private SessionPlace place;
        private LiveSession session;

        private PlacedSessionBuilder() {}

        public PlacedSessionBuilder place(SessionPlace place) {
            this.place = place;
            return this;
        }

        public PlacedSessionBuilder session(LiveSession session) {
            this.session = session;
            return this;
        }

        public PlacedSession build() {
            return new PlacedSession(place, session);
        }
    }
}

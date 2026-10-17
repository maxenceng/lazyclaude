package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Marker;

/** The live sessions of one place outside this project. That they are the place's, and their order, are the manager's. */
public record ElsewhereGroup(SessionPlace place, List<LiveSession> sessions) {

    public ElsewhereGroup {
        Assert.notNull("place", place);
        Assert.notNull("sessions", sessions);
    }

    /** What the place asks of you: the strongest marker among its sessions. */
    public Optional<Marker> strongestMarker() {
        return Marker.strongest(sessions.stream().flatMap(session -> session.state().marker().stream()).toList());
    }

    public static ElsewhereGroupBuilder builder() {
        return new ElsewhereGroupBuilder();
    }

    public static final class ElsewhereGroupBuilder {
        private SessionPlace place;
        private List<LiveSession> sessions;

        private ElsewhereGroupBuilder() {}

        public ElsewhereGroupBuilder place(SessionPlace place) {
            this.place = place;
            return this;
        }

        public ElsewhereGroupBuilder sessions(List<LiveSession> sessions) {
            this.sessions = sessions;
            return this;
        }

        public ElsewhereGroup build() {
            return new ElsewhereGroup(place, sessions);
        }
    }
}

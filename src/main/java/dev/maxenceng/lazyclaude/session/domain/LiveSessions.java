package dev.maxenceng.lazyclaude.session.domain;

import java.time.LocalDateTime;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionAge;

/** The live sessions of one project, newest first, with the clock their ages are read from. */
public record LiveSessions(List<LiveSession> sessions, SessionClock clock) implements Sessions {

    public LiveSessions {
        Assert.notNull("sessions", sessions);
        Assert.notNull("clock", clock);
    }

    public SessionAge ageOf(LiveSession session) {
        return clock.ageOf(session.startedAt());
    }

    public LocalDateTime startedLocally(LiveSession session) {
        return clock.localTimeOf(session.startedAt());
    }

    public static LiveSessionsBuilder builder() {
        return new LiveSessionsBuilder();
    }

    public static final class LiveSessionsBuilder {
        private List<LiveSession> sessions;
        private SessionClock clock;

        private LiveSessionsBuilder() {}

        public LiveSessionsBuilder sessions(List<LiveSession> sessions) {
            this.sessions = sessions;
            return this;
        }

        public LiveSessionsBuilder clock(SessionClock clock) {
            this.clock = clock;
            return this;
        }

        public LiveSessions build() {
            return new LiveSessions(sessions, clock);
        }
    }
}

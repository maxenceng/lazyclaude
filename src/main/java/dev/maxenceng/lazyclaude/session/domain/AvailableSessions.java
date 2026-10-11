package dev.maxenceng.lazyclaude.session.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * The sessions of one project that could be read: the live ones, then the past ones, with the clock their ages are read
 * from. A session is one or the other, never both; the order is the manager's.
 */
public record AvailableSessions(List<LiveSession> live, List<PastSession> past, SessionClock clock)
        implements Sessions {

    public AvailableSessions {
        Assert.notNull("live sessions", live);
        Assert.notNull("past sessions", past);
        Assert.notNull("clock", clock);
        Set<SessionId> liveIds = live.stream().map(LiveSession::id).collect(Collectors.toSet());
        past.stream().map(PastSession::id).filter(liveIds::contains).findFirst().ifPresent(id -> {
            throw new SessionListedTwiceException(id);
        });
    }

    public SessionAge ageOf(LiveSession session) {
        return clock.ageOf(session.startedAt());
    }

    public LocalDateTime startedLocally(LiveSession session) {
        return clock.localTimeOf(session.startedAt());
    }

    public SessionAge ageOf(PastSession session) {
        return clock.ageOf(session.lastActivity());
    }

    public LocalDateTime lastActiveLocally(PastSession session) {
        return clock.localTimeOf(session.lastActivity());
    }

    public LocalDateTime startedLocally(PastSession session) {
        return clock.localTimeOf(session.startedAt());
    }

    public static AvailableSessionsBuilder builder() {
        return new AvailableSessionsBuilder();
    }

    public static final class AvailableSessionsBuilder {
        private List<LiveSession> live;
        private List<PastSession> past;
        private SessionClock clock;

        private AvailableSessionsBuilder() {}

        public AvailableSessionsBuilder live(List<LiveSession> live) {
            this.live = live;
            return this;
        }

        public AvailableSessionsBuilder past(List<PastSession> past) {
            this.past = past;
            return this;
        }

        public AvailableSessionsBuilder clock(SessionClock clock) {
            this.clock = clock;
            return this;
        }

        public AvailableSessions build() {
            return new AvailableSessions(live, past, clock);
        }
    }
}

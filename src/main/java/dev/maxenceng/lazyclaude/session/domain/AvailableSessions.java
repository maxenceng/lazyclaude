package dev.maxenceng.lazyclaude.session.domain;

import java.time.LocalDateTime;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionAge;

/**
 * The sessions of one project that could be read: the live ones, then the past ones, the hosted ones in tab order, with the clock their ages are read
 * from. That a session is one or the other, never both, and the order, are the manager's.
 */
public record AvailableSessions(
        List<LiveSession> live, List<PastSession> past, List<OpenHostedSession> hosted, SessionClock clock)
        implements Sessions {

    public AvailableSessions {
        Assert.notNull("live sessions", live);
        Assert.notNull("past sessions", past);
        Assert.notNull("hosted tabs", hosted);
        Assert.notNull("clock", clock);
    }

    @Override
    public AvailableSessions hosting(HostedSessions open) {
        List<LiveSession> owned =
                live.stream().map(session -> session.ownedAmong(open.pids())).toList();
        return builder()
                .live(owned)
                .past(past)
                .hosted(open.sessions().stream()
                        .map(session -> OpenHostedSession.builder()
                                .session(session)
                                .phase(session.phaseAmong(owned))
                                .build())
                        .toList())
                .clock(clock)
                .build();
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
        private List<OpenHostedSession> hosted = List.of();
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

        public AvailableSessionsBuilder hosted(List<OpenHostedSession> hosted) {
            this.hosted = hosted;
            return this;
        }

        public AvailableSessionsBuilder clock(SessionClock clock) {
            this.clock = clock;
            return this;
        }

        public AvailableSessions build() {
            return new AvailableSessions(live, past, hosted, clock);
        }
    }
}

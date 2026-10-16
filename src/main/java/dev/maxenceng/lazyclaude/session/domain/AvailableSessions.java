package dev.maxenceng.lazyclaude.session.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.StrongLink;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

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
        List<LiveSession> owned = live.stream().map(session -> ownedAmong(open, session)).toList();
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

    private static LiveSession ownedAmong(HostedSessions open, LiveSession session) {
        LiveSession owned = session.ownedAmong(open.pids());
        return open.withPid(session.pid())
                .flatMap(HostedSession::startingLink)
                .map(owned::linkedBy)
                .orElse(owned);
    }

    @Override
    public boolean ranVerb(LinkedTicketId ticket, Verb verb) {
        LinkVerb ran = new LinkVerb(verb.label());
        return Stream.concat(
                        live.stream().map(LiveSession::links), past.stream().map(PastSession::links))
                        .anyMatch(links -> TicketLink.to(links, ticket)
                                .filter(link -> link instanceof StrongLink strong && strong.verb().equals(ran))
                                .isPresent())
                || hosted.stream().anyMatch(open -> open.session().runs(ticket, verb));
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

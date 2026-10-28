package dev.maxenceng.lazyclaude.session.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * The sessions of one project: the hosted ones in tab order, each starting, live or unknown, then the past ones, with the clock their
 * ages are read from. That a session is one or the other, never both, is the manager's.
 */
public record AvailableSessions(List<PastSession> past, List<OpenHostedSession> hosted, SessionClock clock) {

    private static final Comparator<LiveSession> NEWEST_FIRST = Comparator.comparing(LiveSession::startedAt)
            .reversed()
            .thenComparing(session -> session.id().value());

    public AvailableSessions {
        Assert.notNull("past sessions", past);
        Assert.notNull("hosted tabs", hosted);
        Assert.notNull("clock", clock);
    }

    /** The hosted sessions that have something to show, newest start first. */
    public List<LiveSession> live() {
        return hosted.stream()
                .flatMap(open -> open.phase().live().stream())
                .sorted(NEWEST_FIRST)
                .toList();
    }

    /** The names of the hosted sessions whose directory is {@code worktree} or below it, those still starting included. */
    public List<SessionName> namesIn(WorkingDirectory worktree) {
        return hosted.stream()
                .filter(open -> open.session().runsIn(worktree))
                .map(OpenHostedSession::name)
                .toList();
    }

    public boolean ranVerb(LinkedTicketId ticket, Verb verb) {
        LinkVerb ran = new LinkVerb(verb.label());
        return Stream.concat(
                        live().stream().map(LiveSession::links), past.stream().map(PastSession::links))
                        .anyMatch(links -> TicketLink.to(links, ticket)
                                .filter(link -> link.verb().equals(ran))
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
        private List<PastSession> past;
        private List<OpenHostedSession> hosted = List.of();
        private SessionClock clock;

        private AvailableSessionsBuilder() {}

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
            return new AvailableSessions(past, hosted, clock);
        }
    }
}

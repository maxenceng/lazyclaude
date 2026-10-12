package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * Neither the registry nor {@code claude agents --json} answered; lazyclaude starts anyway and says why. The past sessions
 * are not shown either: without the live ones, a session still running cannot be told from its past row. The hosted ones
 * are still lazyclaude's own, so their tabs stay open, unread for want of a registry to say how they stand.
 */
public record UnavailableSessions(Reason reason, List<OpenHostedSession> hosted) implements Sessions {

    public UnavailableSessions {
        Assert.notNull("reason", reason);
        Assert.notNull("hosted tabs", hosted);
    }

    @Override
    public UnavailableSessions hosting(HostedSessions open) {
        return builder()
                .reason(reason)
                .hosted(open.sessions().stream()
                        .map(session -> OpenHostedSession.builder()
                                .session(session)
                                .phase(new UnreadPhase())
                                .build())
                        .toList())
                .build();
    }

    public static UnavailableSessionsBuilder builder() {
        return new UnavailableSessionsBuilder();
    }

    public static final class UnavailableSessionsBuilder {
        private Reason reason;
        private List<OpenHostedSession> hosted = List.of();

        private UnavailableSessionsBuilder() {}

        public UnavailableSessionsBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnavailableSessionsBuilder hosted(List<OpenHostedSession> hosted) {
            this.hosted = hosted;
            return this;
        }

        public UnavailableSessions build() {
            return new UnavailableSessions(reason, hosted);
        }
    }
}

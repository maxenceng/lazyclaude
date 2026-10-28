package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/** An open hosted session and where it stands: starting, live, or unknown. */
public record OpenHostedSession(HostedSession session, HostedPhase phase) {

    public OpenHostedSession {
        Assert.notNull("session", session);
        Assert.notNull("phase", phase);
    }

    /** The session it runs now: the one its registry record names, else the one it was launched with. */
    public SessionId id() {
        return phase.live().map(LiveSession::id).orElseGet(session::id);
    }

    /** What it is called: the name its registry record gave it, else the one it was launched with. */
    public SessionName name() {
        return phase.live().map(LiveSession::name).orElseGet(session::name);
    }

    /** This session with its live session linked to {@code links}. */
    public OpenHostedSession linkedTo(List<TicketLink> links) {
        return phase.live()
                .map(live -> new OpenHostedSession(session, phase.showing(live.linkedTo(links))))
                .orElse(this);
    }

    public static OpenHostedSessionBuilder builder() {
        return new OpenHostedSessionBuilder();
    }

    public static final class OpenHostedSessionBuilder {
        private HostedSession session;
        private HostedPhase phase;

        private OpenHostedSessionBuilder() {}

        public OpenHostedSessionBuilder session(HostedSession session) {
            this.session = session;
            return this;
        }

        public OpenHostedSessionBuilder phase(HostedPhase phase) {
            this.phase = phase;
            return this;
        }

        public OpenHostedSession build() {
            return new OpenHostedSession(session, phase);
        }
    }
}

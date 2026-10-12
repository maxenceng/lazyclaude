package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An open hosted session and where it stands: starting, or live. */
public record OpenHostedSession(HostedSession session, HostedPhase phase) {

    public OpenHostedSession {
        Assert.notNull("session", session);
        Assert.notNull("phase", phase);
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

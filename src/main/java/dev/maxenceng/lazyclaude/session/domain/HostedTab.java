package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A hosted session and where it stands, as one tab of the dock shows it. */
public record HostedTab(HostedSession session, HostedPhase phase) {

    public HostedTab {
        Assert.notNull("session", session);
        Assert.notNull("phase", phase);
    }

    public static HostedTabBuilder builder() {
        return new HostedTabBuilder();
    }

    public static final class HostedTabBuilder {
        private HostedSession session;
        private HostedPhase phase;

        private HostedTabBuilder() {}

        public HostedTabBuilder session(HostedSession session) {
            this.session = session;
            return this;
        }

        public HostedTabBuilder phase(HostedPhase phase) {
            this.phase = phase;
            return this;
        }

        public HostedTab build() {
            return new HostedTab(session, phase);
        }
    }
}

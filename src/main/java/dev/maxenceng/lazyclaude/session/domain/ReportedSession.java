package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

/** What Claude Code's registry record says of a hosted session: its status is still Claude Code's, not yet one of lazyclaude's states. */
public record ReportedSession(
        SessionName name,
        StartedAt startedAt,
        ClaudeVersion version,
        ReportedStatus status,
        WaitingFor waitingFor) {

    public ReportedSession {
        Assert.notNull("name", name);
        Assert.notNull("started at", startedAt);
        Assert.notNull("version", version);
        Assert.notNull("status", status);
        Assert.notNull("waiting for", waitingFor);
    }

    /** A waiting session blocked on a permission prompt asks for permission; any other reason asks for input. */
    public SessionState state() {
        return switch (status) {
            case BUSY, SHELL -> new Busy();
            case IDLE -> new YourTurn();
            case WAITING -> waiting();
            case UNKNOWN -> new Unknown();
        };
    }

    private SessionState waiting() {
        return waitingFor instanceof WaitingReason reason && reason.isPermissionPrompt()
                ? new WaitingForPermission(reason)
                : new WaitingForInput(waitingFor);
    }

    public static ReportedSessionBuilder builder() {
        return new ReportedSessionBuilder();
    }

    public static final class ReportedSessionBuilder {
        private SessionName name;
        private StartedAt startedAt;
        private ClaudeVersion version;
        private ReportedStatus status;
        private WaitingFor waitingFor;

        private ReportedSessionBuilder() {}

        public ReportedSessionBuilder name(SessionName name) {
            this.name = name;
            return this;
        }

        public ReportedSessionBuilder startedAt(StartedAt startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ReportedSessionBuilder version(ClaudeVersion version) {
            this.version = version;
            return this;
        }

        public ReportedSessionBuilder status(ReportedStatus status) {
            this.status = status;
            return this;
        }

        public ReportedSessionBuilder waitingFor(WaitingFor waitingFor) {
            this.waitingFor = waitingFor;
            return this;
        }

        public ReportedSession build() {
            return new ReportedSession(name, startedAt, version, status, waitingFor);
        }
    }
}

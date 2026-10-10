package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * A session whose process runs, as its source reported it after the liveness check: its status is still
 * Claude Code's, not yet one of lazyclaude's states.
 */
public record ReportedSession(
        SessionId id,
        ProcessId pid,
        SessionName name,
        WorkingDirectory directory,
        StartedAt startedAt,
        ClaudeVersion version,
        ReportedStatus status,
        WaitingFor waitingFor,
        Entrypoint entrypoint) {

    public ReportedSession {
        Assert.notNull("id", id);
        Assert.notNull("pid", pid);
        Assert.notNull("name", name);
        Assert.notNull("directory", directory);
        Assert.notNull("started at", startedAt);
        Assert.notNull("version", version);
        Assert.notNull("status", status);
        Assert.notNull("waiting for", waitingFor);
        Assert.notNull("entrypoint", entrypoint);
    }

    boolean isSdkCli() {
        return entrypoint instanceof SdkCliEntrypoint;
    }

    public static ReportedSessionBuilder builder() {
        return new ReportedSessionBuilder();
    }

    public static final class ReportedSessionBuilder {
        private SessionId id;
        private ProcessId pid;
        private SessionName name;
        private WorkingDirectory directory;
        private StartedAt startedAt;
        private ClaudeVersion version;
        private ReportedStatus status;
        private WaitingFor waitingFor;
        private Entrypoint entrypoint;

        private ReportedSessionBuilder() {}

        public ReportedSessionBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public ReportedSessionBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public ReportedSessionBuilder name(SessionName name) {
            this.name = name;
            return this;
        }

        public ReportedSessionBuilder directory(WorkingDirectory directory) {
            this.directory = directory;
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

        public ReportedSessionBuilder entrypoint(Entrypoint entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        public ReportedSession build() {
            return new ReportedSession(id, pid, name, directory, startedAt, version, status, waitingFor, entrypoint);
        }
    }
}

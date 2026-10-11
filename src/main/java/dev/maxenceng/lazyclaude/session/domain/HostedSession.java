package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * A {@code claude} process lazyclaude owns: starting while no registry record carries its pid, then live as the session
 * that record describes, until its process exits and it is dropped.
 */
public record HostedSession(HostedSessionId id, ProcessId pid, WorkingDirectory directory) {

    public HostedSession {
        Assert.notNull("hosted session id", id);
        Assert.notNull("pid", pid);
        Assert.notNull("directory", directory);
    }

    /** Where the session stands among the live sessions the registry lists: registered as the one with its pid, else still starting. */
    public HostedPhase phaseAmong(List<LiveSession> live) {
        return live.stream()
                .filter(session -> session.pid().equals(pid))
                .<HostedPhase>map(RegisteredPhase::new)
                .findFirst()
                .orElseGet(StartingPhase::new);
    }

    public static HostedSessionBuilder builder() {
        return new HostedSessionBuilder();
    }

    public static final class HostedSessionBuilder {
        private HostedSessionId id;
        private ProcessId pid;
        private WorkingDirectory directory;

        private HostedSessionBuilder() {}

        public HostedSessionBuilder id(HostedSessionId id) {
            this.id = id;
            return this;
        }

        public HostedSessionBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public HostedSessionBuilder directory(WorkingDirectory directory) {
            this.directory = directory;
            return this;
        }

        public HostedSession build() {
            return new HostedSession(id, pid, directory);
        }
    }
}

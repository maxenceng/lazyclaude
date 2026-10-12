package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * A {@code claude} process lazyclaude owns: starting while no registry record carries its pid, then live as the session
 * that record describes, until its process exits and it is dropped.
 */
public record HostedSession(HostedSessionId id, ProcessId pid, WorkingDirectory directory, LaunchCommand command) {

    public HostedSession {
        Assert.notNull("hosted session id", id);
        Assert.notNull("pid", pid);
        Assert.notNull("directory", directory);
        Assert.notNull("command", command);
    }

    /** Whether this is {@code claude --resume} of {@code session}, which a second resume must not run beside. */
    public boolean resumes(SessionId session) {
        return command instanceof ResumeClaude resume && resume.session().equals(session);
    }

    /** Where the session stands among the live sessions the registry lists: live as the one with its pid, else still starting. */
    public HostedPhase phaseAmong(List<LiveSession> live) {
        return live.stream()
                .filter(session -> session.pid().equals(pid))
                .<HostedPhase>map(LivePhase::new)
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
        private LaunchCommand command;

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

        public HostedSessionBuilder command(LaunchCommand command) {
            this.command = command;
            return this;
        }

        public HostedSession build() {
            return new HostedSession(id, pid, directory, command);
        }
    }
}

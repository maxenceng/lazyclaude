package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionDirectory;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.StartedAt;

/** A Claude Code session whose process runs, as its source reported it after the liveness check. */
public record LiveSession(
        SessionId id,
        ProcessId pid,
        SessionName name,
        SessionDirectory directory,
        StartedAt startedAt,
        ClaudeVersion version,
        SessionState state,
        Entrypoint entrypoint) {

    public LiveSession {
        Assert.notNull("id", id);
        Assert.notNull("pid", pid);
        Assert.notNull("name", name);
        Assert.notNull("directory", directory);
        Assert.notNull("started at", startedAt);
        Assert.notNull("version", version);
        Assert.notNull("state", state);
        Assert.notNull("entrypoint", entrypoint);
    }

    boolean isSdkCli() {
        return entrypoint instanceof SdkCliEntrypoint;
    }

    public static LiveSessionBuilder builder() {
        return new LiveSessionBuilder();
    }

    public static final class LiveSessionBuilder {
        private SessionId id;
        private ProcessId pid;
        private SessionName name;
        private SessionDirectory directory;
        private StartedAt startedAt;
        private ClaudeVersion version;
        private SessionState state;
        private Entrypoint entrypoint;

        private LiveSessionBuilder() {}

        public LiveSessionBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public LiveSessionBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public LiveSessionBuilder name(SessionName name) {
            this.name = name;
            return this;
        }

        public LiveSessionBuilder directory(SessionDirectory directory) {
            this.directory = directory;
            return this;
        }

        public LiveSessionBuilder startedAt(StartedAt startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public LiveSessionBuilder version(ClaudeVersion version) {
            this.version = version;
            return this;
        }

        public LiveSessionBuilder state(SessionState state) {
            this.state = state;
            return this;
        }

        public LiveSessionBuilder entrypoint(Entrypoint entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        public LiveSession build() {
            return new LiveSession(id, pid, name, directory, startedAt, version, state, entrypoint);
        }
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * A live session of the project, with the state lazyclaude draws from what Claude Code reported and the tickets its
 * transcript links it to, none while it has no transcript yet.
 */
public record LiveSession(
        SessionId id,
        ProcessId pid,
        SessionName name,
        WorkingDirectory directory,
        StartedAt startedAt,
        ClaudeVersion version,
        SessionState state,
        List<TicketLink> links,
        Hosting hosting) {

    public LiveSession {
        Assert.notNull("id", id);
        Assert.notNull("pid", pid);
        Assert.notNull("name", name);
        Assert.notNull("directory", directory);
        Assert.notNull("started at", startedAt);
        Assert.notNull("version", version);
        Assert.notNull("state", state);
        Assert.notNull("links", links);
        Assert.notNull("hosting", hosting);
    }

    public LiveSession linkedTo(List<TicketLink> links) {
        return new LiveSession(id, pid, name, directory, startedAt, version, state, links, hosting);
    }

    /** This session as lazyclaude knows it to be owned: hosted when its pid is one of {@code hostedPids}, else outside. */
    public LiveSession ownedAmong(List<ProcessId> hostedPids) {
        Hosting owner = hostedPids.contains(pid) ? Hosting.HOSTED : Hosting.OUTSIDE;
        return new LiveSession(id, pid, name, directory, startedAt, version, state, links, owner);
    }

    public static LiveSessionBuilder builder() {
        return new LiveSessionBuilder();
    }

    public static final class LiveSessionBuilder {
        private SessionId id;
        private ProcessId pid;
        private SessionName name;
        private WorkingDirectory directory;
        private StartedAt startedAt;
        private ClaudeVersion version;
        private SessionState state;
        private List<TicketLink> links = List.of();
        private Hosting hosting = Hosting.OUTSIDE;

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

        public LiveSessionBuilder directory(WorkingDirectory directory) {
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

        public LiveSessionBuilder links(List<TicketLink> links) {
            this.links = links;
            return this;
        }

        public LiveSessionBuilder hosting(Hosting hosting) {
            this.hosting = hosting;
            return this;
        }

        public LiveSession build() {
            return new LiveSession(id, pid, name, directory, startedAt, version, state, links, hosting);
        }
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** The hosted sessions that are open, in the order they were started, which is the order of the dock's tabs. */
public record HostedSessions(List<HostedSession> sessions) {

    public HostedSessions {
        Assert.notNull("hosted sessions", sessions);
    }

    public List<ProcessId> pids() {
        return sessions.stream().map(HostedSession::pid).toList();
    }
}

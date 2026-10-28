package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.startingIn;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;

import java.util.List;

public final class WorktreeManagerFixture {

    private WorktreeManagerFixture() {}

    public static AvailableSessions noSessionsInTheWorktree() {
        return liveSessionsOf(List.of());
    }

    public static AvailableSessions oneSessionInTheWorktree() {
        return availableSessionsHosting(List.of(), List.of(startingIn(worktreeDirectory())));
    }
}

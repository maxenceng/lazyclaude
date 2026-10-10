package dev.maxenceng.lazyclaude.session.published;

import java.nio.file.Path;

public final class SessionDirectoryFixture {

    private SessionDirectoryFixture() {}

    public static SessionDirectory sessionDirectory() {
        return new SessionDirectory(Path.of("/work/app"));
    }

    public static SessionDirectory worktreeDirectory() {
        return new SessionDirectory(Path.of("/work/app.worktrees/SESSION-1-live-sessions"));
    }

    public static SessionDirectory otherProjectDirectory() {
        return new SessionDirectory(Path.of("/work/other"));
    }

    public static SessionDirectory outsideGitDirectory() {
        return new SessionDirectory(Path.of("/tmp/scratch"));
    }
}

package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;

public final class WorkingDirectoryFixture {

    private WorkingDirectoryFixture() {}

    public static WorkingDirectory workingDirectoryAt(String path) {
        return new WorkingDirectory(Path.of(path));
    }

    public static WorkingDirectory workingDirectory() {
        return new WorkingDirectory(Path.of("/work/app/src"));
    }

    public static WorkingDirectory projectRootDirectory() {
        return new WorkingDirectory(Path.of("/work/app"));
    }

    public static WorkingDirectory worktreeDirectory() {
        return new WorkingDirectory(Path.of("/work/app.worktrees/PROJ-1-ticket"));
    }

    public static WorkingDirectory otherProjectDirectory() {
        return new WorkingDirectory(Path.of("/work/other"));
    }

}

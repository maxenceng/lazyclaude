package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;

public final class WorkingDirectoryFixture {

    private WorkingDirectoryFixture() {}

    public static WorkingDirectory workingDirectory() {
        return new WorkingDirectory(Path.of("/work/app/src"));
    }
}

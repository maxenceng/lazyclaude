package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;

import java.nio.file.Path;

public final class WorktreeRepositoryFixture {

    private WorktreeRepositoryFixture() {}

    public static Path ticketWorktreePath(Path temp) {
        return temp.resolve("app.worktrees").resolve(branchName().value());
    }
}

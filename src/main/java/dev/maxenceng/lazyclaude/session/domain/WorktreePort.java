package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Where a project's worktrees are read from and added to. */
public interface WorktreePort {

    /**
     * The checkout of {@code project}'s repository that has {@code branch} checked out, the root checkout included.
     *
     * @throws WorktreeUnavailableException when git cannot list the worktrees
     */
    Optional<WorkingDirectory> checkedOutAt(Project project, BranchName branch);

    /**
     * Whether the repository has a local branch named {@code branch}.
     *
     * @throws WorktreeUnavailableException when git cannot say
     */
    boolean branchExists(Project project, BranchName branch);

    /**
     * Adds a worktree at {@code at} with the existing {@code branch} checked out.
     *
     * @throws WorktreeUnavailableException when git refuses
     */
    void addOnBranch(Project project, BranchName branch, WorkingDirectory at);

    /**
     * Adds a worktree at {@code at} on a new branch {@code branch} started from {@code main}.
     *
     * @throws WorktreeUnavailableException when git refuses
     */
    void addOnNewBranch(Project project, BranchName branch, WorkingDirectory at);
}

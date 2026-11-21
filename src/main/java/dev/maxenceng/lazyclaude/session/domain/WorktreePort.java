package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.review.published.HeadCommit;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Where a project's worktrees are read from, added to and removed from. */
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
     * Whether the project's remote has a branch named {@code branch}. The remote is asked, so this goes over the network.
     *
     * @throws WorktreeUnavailableException when the remote cannot be asked: offline, refused, or no such remote
     */
    boolean branchOnRemote(Project project, BranchName branch);

    /**
     * Fetches {@code branch} from the project's remote and adds a worktree at {@code at} on a new local {@code branch} that starts at
     * the remote's tip of it and tracks it.
     *
     * @throws WorktreeUnavailableException when the fetch fails or git refuses the worktree
     */
    void addOnRemoteBranch(Project project, BranchName branch, WorkingDirectory at);

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

    /**
     * Whether git lists a worktree of {@code project}'s repository at {@code at}, whose directory still exists.
     *
     * @throws WorktreeUnavailableException when git cannot list the worktrees
     */
    boolean isWorktreeAt(Project project, WorkingDirectory at);

    /**
     * The paths {@code git status --porcelain} reports in {@code worktree}: modified, staged and untracked ones alike.
     *
     * @throws WorktreeUnavailableException when git cannot say
     */
    List<DirtyPath> dirtyPaths(WorkingDirectory worktree);

    /**
     * The commits {@code worktree}'s checkout holds that are neither {@code mergedHead} nor behind it and that no remote ref has.
     *
     * @throws WorktreeUnavailableException when git cannot say
     */
    List<CommitId> commitsBeyond(WorkingDirectory worktree, HeadCommit mergedHead);

    /**
     * Removes the worktree at {@code worktree}; its branch stays. Git refuses a worktree with changes in it, and nothing is forced.
     *
     * @throws WorktreeNotRemovedException when git refuses, with git's message
     */
    void remove(Project project, WorkingDirectory worktree);
}

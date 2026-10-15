package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where the copies of a ticket file that live off the root checkout are read from: other worktrees, and the branches themselves. */
public interface BranchCopyPort {

    /**
     * Every worktree of {@code project}'s repository, the root checkout included.
     *
     * @throws BranchCopyUnavailableException when git cannot list them
     */
    List<Worktree> worktrees(Project project);

    /**
     * The backlog file {@code file} as it stands in {@code worktree}, uncommitted edits included; empty when the worktree has no such
     * file or the file does not claim to be a ticket. A file that is refused is an {@link UnreadableTicket}, not an absence.
     */
    Optional<BacklogEntry> inWorktree(Worktree worktree, FileName file);

    /**
     * The backlog file {@code file} as {@code branch} commits it; empty when there is no such branch, or it has no such file or the file
     * does not claim to be a ticket. A file that is refused is an {@link UnreadableTicket}, not an absence.
     *
     * @throws BranchCopyUnavailableException when the branch exists and git still cannot give the file
     */
    Optional<BacklogEntry> onBranch(Project project, TicketBranch branch, FileName file);
}

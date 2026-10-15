package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where what a ticket branch changes from {@code main} is read from. */
public interface BranchChangesPort {

    /** The files {@code branch} changes from {@code main}; unread when git cannot say, as for a branch that does not exist. */
    BranchChanges changesOf(Project project, TicketBranch branch);
}

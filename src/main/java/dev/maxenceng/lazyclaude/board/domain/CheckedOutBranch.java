package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A worktree on a branch, whatever that branch is named: a ticket branch, the trunk, or any other. */
public record CheckedOutBranch(String name) implements WorktreeHead {

    public CheckedOutBranch {
        Assert.notBlank("checked-out branch", name);
    }

    /** Whether the branch checked out is {@code branch}, by name and nothing looser. */
    public boolean is(TicketBranch branch) {
        return name.equals(branch.value());
    }
}

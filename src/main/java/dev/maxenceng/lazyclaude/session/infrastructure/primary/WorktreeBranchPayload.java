package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What the desktop window asks of a ticket worktree with: the ticket's branch, which is all it knows of it. */
public record WorktreeBranchPayload(String branch) {

    public WorktreeBranchPayload {
        Assert.notBlank("branch", branch);
    }
}

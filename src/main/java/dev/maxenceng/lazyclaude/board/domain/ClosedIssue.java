package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An issue closed, with the reason GitHub gives for it. */
public record ClosedIssue(ClosedReason reason) implements IssueState {

    public ClosedIssue {
        Assert.notNull("reason", reason);
    }
}

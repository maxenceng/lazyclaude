package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An issue closed, with the reason GitHub gives for it and when it closed. */
public record ClosedIssue(ClosedReason reason, ClosedAt closedAt) implements IssueState {

    public ClosedIssue {
        Assert.notNull("reason", reason);
        Assert.notNull("closedAt", closedAt);
    }

    public static ClosedIssueBuilder builder() {
        return new ClosedIssueBuilder();
    }

    public static final class ClosedIssueBuilder {
        private ClosedReason reason;
        private ClosedAt closedAt;

        private ClosedIssueBuilder() {}

        public ClosedIssueBuilder reason(ClosedReason reason) {
            this.reason = reason;
            return this;
        }

        public ClosedIssueBuilder closedAt(ClosedAt closedAt) {
            this.closedAt = closedAt;
            return this;
        }

        public ClosedIssue build() {
            return new ClosedIssue(reason, closedAt);
        }
    }
}

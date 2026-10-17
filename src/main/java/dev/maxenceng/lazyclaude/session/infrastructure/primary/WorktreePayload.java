package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.MergedHead;
import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;

/**
 * What a ticket worktree is asked about or removed with: the project and its ticket ids, as for any read of the sessions, the ticket
 * branch, and the commit {@code review} last read that branch's pull request merged at, or none.
 */
public record WorktreePayload(SessionPayload sessions, String branch, MergedHead mergedHead) {

    public WorktreePayload {
        Assert.notNull("sessions", sessions);
        Assert.notNull("branch", branch);
        Assert.notNull("merged head", mergedHead);
    }

    TicketWorktree toDomain() {
        return TicketWorktree.builder()
                .project(sessions.toDomain())
                .branch(new BranchName(branch))
                .build();
    }
}

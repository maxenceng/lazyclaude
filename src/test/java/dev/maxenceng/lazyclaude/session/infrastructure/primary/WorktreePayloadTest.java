package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketWorktree;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.worktreePayload;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorktreePayloadTest {

    @Test
    void shouldNameTheTicketWorktreeOfItsBranchInTheProject() {
        assertThat(worktreePayload(headCommit()).toDomain()).isEqualTo(ticketWorktree());
    }
}

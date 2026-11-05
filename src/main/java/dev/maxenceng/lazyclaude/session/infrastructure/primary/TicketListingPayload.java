package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** The row is listed under the ticket whose branch this is. */
public record TicketListingPayload(String branch) implements ListingPayload {

    public TicketListingPayload {
        Assert.notNull("branch", branch);
    }

    @Override
    public Optional<TicketWorktree> worktreeIn(Project project) {
        return Optional.of(TicketWorktree.builder().project(project).branch(new BranchName(branch)).build());
    }
}

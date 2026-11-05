package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** The row is listed under no ticket. */
public record NoTicketListingPayload() implements ListingPayload {

    @Override
    public Optional<TicketWorktree> worktreeIn(Project project) {
        return Optional.empty();
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** What a past session's row is listed under: a ticket, whose branch names its worktree, or no ticket. */
public sealed interface ListingPayload permits TicketListingPayload, NoTicketListingPayload {

    Optional<TicketWorktree> worktreeIn(Project project);
}

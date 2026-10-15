package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.TicketStart;
import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * What {@link HostedSessionCommand} is asked a session from a ticket with: the project root, the ticket and its branch, the verb to
 * run on it, and the size of the pane it is drawn in.
 */
public record TicketHostedPayload(Path root, LinkedTicketId ticket, TicketBranch branch, Verb verb, PaneSize size) {

    public TicketHostedPayload {
        Assert.notNull("ticket", ticket);
        Assert.notNull("branch", branch);
        Assert.notNull("verb", verb);
        Assert.notNull("pane size", size);
    }

    TicketStart toDomain() {
        return TicketStart.builder()
                .worktree(TicketWorktree.builder()
                        .project(new Project(root))
                        .branch(new BranchName(branch.value()))
                        .build())
                .ticket(ticket)
                .verb(verb)
                .build();
    }
}

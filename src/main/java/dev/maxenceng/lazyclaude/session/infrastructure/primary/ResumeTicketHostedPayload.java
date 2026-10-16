package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.TicketWorktree;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * What {@link HostedSessionCommand} is asked a resume of a ticket's past session with: the session, the directory its transcript
 * recorded, the project root and the ticket's branch, whose worktree is added back when the recorded directory is it and is gone,
 * and the pane's size.
 */
public record ResumeTicketHostedPayload(
        SessionId session, Path recordedDirectory, Path root, String branch, PaneSize size) {

    public ResumeTicketHostedPayload {
        Assert.notNull("session", session);
        Assert.notNull("branch", branch);
        Assert.notNull("pane size", size);
    }

    WorkingDirectory recorded() {
        return new WorkingDirectory(recordedDirectory);
    }

    TicketWorktree worktree() {
        return TicketWorktree.builder()
                .project(new Project(root))
                .branch(new BranchName(branch))
                .build();
    }
}

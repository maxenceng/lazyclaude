package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotClearException;
import dev.maxenceng.lazyclaude.session.domain.WorktreeNotRemovedException;
import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;
import dev.maxenceng.lazyclaude.session.published.Reason;

/** What the shell asks of {@code session} to remove a ticket's worktree, which runs {@code git}, so not on the render thread. */
public final class TicketWorktreeCommand {

    private final SessionApplicationService sessions;

    public TicketWorktreeCommand(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /**
     * Removes the ticket worktree after judging it again, keeping the branch; refused, with what stands in it, when anything does,
     * and failed, with git's message, when git cannot; never a failure.
     */
    public WorktreeRemovalView remove(WorktreePayload payload) {
        try {
            sessions.removeWorktree(
                    payload.sessions().toDomain(),
                    payload.sessions().tickets(),
                    payload.toDomain(),
                    payload.mergedHead());
            return new RemovedWorktreeView();
        } catch (WorktreeNotClearException refused) {
            return new RefusedRemovalView(WorktreeStateView.from(refused.state()));
        } catch (WorktreeNotRemovedException | WorktreeUnavailableException failed) {
            return new FailedRemovalView(new Reason(failed.getMessage()));
        }
    }
}

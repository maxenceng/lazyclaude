package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** {@code git worktree remove} failed; the worktree and its branch are as they were, and the message is git's own. */
public final class WorktreeNotRemovedException extends DomainException {

    private static final long serialVersionUID = 1L;

    public WorktreeNotRemovedException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public WorktreeNotRemovedException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

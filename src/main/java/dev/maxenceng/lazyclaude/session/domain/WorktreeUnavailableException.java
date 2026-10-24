package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** Git could not say where a branch is checked out or what stands in a ticket worktree, or could not add one; the message says why. */
public final class WorktreeUnavailableException extends DomainException {

    private static final long serialVersionUID = 1L;

    public WorktreeUnavailableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public WorktreeUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

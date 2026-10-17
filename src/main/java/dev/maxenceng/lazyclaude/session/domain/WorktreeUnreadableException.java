package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** Git could not say what stands in a ticket worktree, so it cannot be judged; nothing was removed, and the message says why. */
public final class WorktreeUnreadableException extends DomainException {

    public WorktreeUnreadableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public WorktreeUnreadableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

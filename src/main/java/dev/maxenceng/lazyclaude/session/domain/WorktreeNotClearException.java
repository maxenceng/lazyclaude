package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A ticket worktree was asked to be removed while something stands in it; nothing was removed, and the state says what. */
public final class WorktreeNotClearException extends DomainException {

    private final transient WorktreeState state;

    public WorktreeNotClearException(WorktreeState state) {
        super(DomainErrorStatus.UNAVAILABLE, "the ticket worktree is not clear to remove");
        this.state = state;
    }

    public WorktreeState state() {
        return state;
    }
}

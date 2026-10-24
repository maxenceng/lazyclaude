package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/**
 * Git could not give what branch copies are read from: the list of worktrees, which leaves no board to read, or one branch's file, which
 * makes that one ticket unreadable. The message says why, for the person reading the board.
 */
public final class BranchCopyUnavailableException extends DomainException {

    private static final long serialVersionUID = 1L;

    public BranchCopyUnavailableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public BranchCopyUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

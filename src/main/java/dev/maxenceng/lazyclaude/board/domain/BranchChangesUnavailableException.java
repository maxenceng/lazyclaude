package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** Git could not be run to say what a ticket branch changes from {@code main}, which leaves no board to read. */
public final class BranchChangesUnavailableException extends DomainException {

    public BranchChangesUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

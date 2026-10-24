package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The command that brings a window forward could not be run or failed; the message says why, for the person reading the status line. */
public final class TerminalJumpFailedException extends DomainException {

    public TerminalJumpFailedException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public TerminalJumpFailedException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

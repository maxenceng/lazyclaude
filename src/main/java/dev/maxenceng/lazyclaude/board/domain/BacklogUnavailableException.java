package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The project's issues cannot be read: GitHub is unreachable, or {@code gh} is missing or not logged in; the board on screen, if any, stays. */
public final class BacklogUnavailableException extends DomainException {

    public BacklogUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }

    public BacklogUnavailableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }
}

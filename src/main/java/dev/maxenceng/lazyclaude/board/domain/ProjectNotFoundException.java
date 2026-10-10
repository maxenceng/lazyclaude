package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The working directory belongs to no project; the message says why. */
public final class ProjectNotFoundException extends DomainException {

    public ProjectNotFoundException(String reason) {
        super(DomainErrorStatus.NOT_FOUND, reason);
    }

    public ProjectNotFoundException(String reason, Throwable cause) {
        super(DomainErrorStatus.NOT_FOUND, reason, cause);
    }
}

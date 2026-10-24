package dev.maxenceng.lazyclaude.shared.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** Whether the working directory belongs to a project could not be told this time; the message says why. */
public final class ProjectUnavailableException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ProjectUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

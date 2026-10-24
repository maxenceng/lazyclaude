package dev.maxenceng.lazyclaude.shared.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The working directory belongs to no project; the message says why. */
public final class ProjectNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ProjectNotFoundException(String reason) {
        super(DomainErrorStatus.NOT_FOUND, reason);
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A running process's environment could not be read; the message says why, for the person reading the status line. */
public final class EnvironmentUnreadableException extends DomainException {

    private static final long serialVersionUID = 1L;

    public EnvironmentUnreadableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public EnvironmentUnreadableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The live sessions could not be read, or placed in their project; the message says why, for the person reading the screen. */
public final class SessionsUnavailableException extends DomainException {

    public SessionsUnavailableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public SessionsUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

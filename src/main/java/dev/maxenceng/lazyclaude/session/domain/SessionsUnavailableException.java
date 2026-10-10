package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A source of sessions could not answer; the message says why, for the person reading the screen. */
public final class SessionsUnavailableException extends DomainException {

    public SessionsUnavailableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public SessionsUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

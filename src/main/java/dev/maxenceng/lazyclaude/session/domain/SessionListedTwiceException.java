package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** A session was given as both live and past; it is one or the other, and the live one hides the past. */
public final class SessionListedTwiceException extends DomainException {

    private static final String MESSAGE = "session %s is listed as both live and past";

    public SessionListedTwiceException(SessionId id) {
        super(DomainErrorStatus.INVALID, MESSAGE.formatted(id.value()));
    }
}

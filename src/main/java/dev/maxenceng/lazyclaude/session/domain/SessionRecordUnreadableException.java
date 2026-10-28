package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/**
 * A hosted session's registry record is there and cannot be used; the message says why, for the person reading the warning.
 * The session shows as unknown and the others keep their state.
 */
public final class SessionRecordUnreadableException extends DomainException {

    public SessionRecordUnreadableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public SessionRecordUnreadableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

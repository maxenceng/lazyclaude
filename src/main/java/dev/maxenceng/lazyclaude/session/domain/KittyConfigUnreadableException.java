package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** kitty's configuration could not be read; the message says why, for the person reading the status line. */
public final class KittyConfigUnreadableException extends DomainException {

    public KittyConfigUnreadableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

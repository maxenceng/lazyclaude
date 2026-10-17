package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** kitty's configuration could not be added to; the message says why, for the person reading the status line. */
public final class KittyConfigUnwritableException extends DomainException {

    public KittyConfigUnwritableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

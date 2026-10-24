package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** kitty's configuration could not be used; the message says why, for the person reading the status line. */
public abstract sealed class KittyConfigException extends DomainException
        permits KittyConfigUnreadableException, KittyConfigUnwritableException {

    private static final long serialVersionUID = 1L;

    KittyConfigException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

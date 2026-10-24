package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** kitty refused a command because remote control is not allowed, or listens on no socket; the message says how it answered. */
public final class RemoteControlOffException extends DomainException {

    private static final long serialVersionUID = 1L;

    public RemoteControlOffException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }
}

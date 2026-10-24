package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** No {@code claude} could be started on a pty; the message says why, for the person reading the status line. */
public final class HostUnavailableException extends DomainException {

    public HostUnavailableException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }

    public HostUnavailableException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}

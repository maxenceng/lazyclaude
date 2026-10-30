package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A new ticket could not be written: the template cannot be read, or GitHub is unreachable, not logged in, or refused the issue; nothing is retried. */
public final class TicketCaptureRefusedException extends DomainException {

    public TicketCaptureRefusedException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }

    public TicketCaptureRefusedException(String reason) {
        super(DomainErrorStatus.UNAVAILABLE, reason);
    }
}

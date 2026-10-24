package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A comment was asked for on a line the diff does not hold, as after the diff was read again and came back shorter. */
public final class DiffLineMissingException extends DomainException {

    private static final long serialVersionUID = 1L;

    public DiffLineMissingException() {
        super(DomainErrorStatus.NOT_FOUND, "that line is no longer in the diff");
    }
}

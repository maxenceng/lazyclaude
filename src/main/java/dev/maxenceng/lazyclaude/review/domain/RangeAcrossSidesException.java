package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A comment's range holds both a removed and an added line, so no one side numbers all of it. The range is left as it was. */
public final class RangeAcrossSidesException extends DomainException {

    private static final long serialVersionUID = 1L;

    public RangeAcrossSidesException() {
        super(DomainErrorStatus.INVALID, "a range must stay on one side of the diff");
    }
}

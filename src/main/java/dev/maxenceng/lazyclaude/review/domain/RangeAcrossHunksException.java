package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A comment's range runs out of its hunk, which GitHub would refuse. The range is left as it was. */
public final class RangeAcrossHunksException extends DomainException {

    public RangeAcrossHunksException() {
        super(DomainErrorStatus.INVALID, "a range must stay inside one hunk");
    }
}

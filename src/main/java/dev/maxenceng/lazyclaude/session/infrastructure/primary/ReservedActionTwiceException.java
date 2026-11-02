package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;

/** A configuration's chords name one reserved action twice, so which chord it takes is not said. */
public final class ReservedActionTwiceException extends DomainException {

    private static final String REASON = "%s is bound twice";

    private final ReservedAction action;

    ReservedActionTwiceException(ReservedAction action) {
        super(DomainErrorStatus.INVALID, REASON.formatted(action.configurationName()));
        Assert.notNull("action", action);
        this.action = action;
    }

    public ReservedAction action() {
        return action;
    }
}

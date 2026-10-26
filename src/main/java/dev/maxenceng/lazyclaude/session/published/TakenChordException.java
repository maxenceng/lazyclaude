package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** A reserved action is bound to one of lazyclaude's fixed chords, which would then stop doing what it does. */
public final class TakenChordException extends DomainException {

    private static final String REASON = "%s is bound to %s, which is lazyclaude's %s";

    private final ReservedAction action;
    private final FixedChord fixed;

    TakenChordException(ReservedAction action, FixedChord fixed) {
        super(DomainErrorStatus.INVALID, REASON.formatted(action.configurationName(), fixed.chord().text(), fixed.purpose()));
        Assert.notNull("fixed chord", fixed);
        this.action = action;
        this.fixed = fixed;
    }

    public ReservedAction action() {
        return action;
    }

    public FixedChord fixed() {
        return fixed;
    }
}

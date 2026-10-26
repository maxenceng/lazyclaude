package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** Two actions are bound to one chord, so one of them could never be pressed. */
public final class DuplicateChordException extends DomainException {

    private static final String REASON = "%s and %s are both bound to %s";

    private final Chord chord;
    private final ReservedAction first;
    private final ReservedAction second;

    DuplicateChordException(Chord chord, ReservedAction first, ReservedAction second) {
        super(DomainErrorStatus.INVALID, REASON.formatted(first.configurationName(), second.configurationName(), chord.text()));
        Assert.notNull("chord", chord);
        this.chord = chord;
        this.first = first;
        this.second = second;
    }

    public Chord chord() {
        return chord;
    }

    public ReservedAction first() {
        return first;
    }

    public ReservedAction second() {
        return second;
    }
}

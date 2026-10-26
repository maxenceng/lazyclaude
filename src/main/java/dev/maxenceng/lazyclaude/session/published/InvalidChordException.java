package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/**
 * The text is not one chord lazyclaude can hold: no modifier or {@code shift} alone, which would steal what the user types into
 * {@code claude}, several keystrokes, or a word that is not a modifier or a key. The message says which.
 */
public final class InvalidChordException extends DomainException {

    public InvalidChordException(String reason) {
        super(DomainErrorStatus.INVALID, reason);
    }
}

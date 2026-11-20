package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.published.Marker;

/** A marker's view was given a strength other than the marker's rank, so the frontends would order it as another marker. */
public final class MarkerStrengthNotItsRankException extends DomainException {

    private static final String REASON = "%s ranks %d, not %d";

    MarkerStrengthNotItsRankException(Marker kind, int strength) {
        super(DomainErrorStatus.INVALID, REASON.formatted(kind, kind.ordinal(), strength));
    }
}

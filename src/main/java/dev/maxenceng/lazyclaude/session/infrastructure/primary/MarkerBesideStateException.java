package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.published.SessionState;

/** A live session's view was given a marker its state does not carry, so the two would tell the frontends different things. */
public final class MarkerBesideStateException extends DomainException {

    private static final String REASON = "a session in %s cannot carry the marker %s";

    MarkerBesideStateException(SessionState state, Optional<MarkerView> marker) {
        super(DomainErrorStatus.INVALID, REASON.formatted(state.getClass().getSimpleName(), marker.map(MarkerView::kind)));
    }
}

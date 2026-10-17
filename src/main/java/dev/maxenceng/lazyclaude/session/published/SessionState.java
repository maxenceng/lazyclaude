package dev.maxenceng.lazyclaude.session.published;

import java.util.Optional;

/** What a session is doing, in lazyclaude's words: working, waiting for your next prompt, blocked on you, or not known. */
public sealed interface SessionState permits Busy, YourTurn, WaitingForPermission, WaitingForInput, Unknown {

    /** What this state asks of you, if anything: busy and unknown ask nothing. */
    default Optional<Marker> marker() {
        return switch (this) {
            case WaitingForPermission permission -> Optional.of(Marker.WAITING_FOR_PERMISSION);
            case WaitingForInput input -> Optional.of(Marker.WAITING_FOR_INPUT);
            case YourTurn yourTurn -> Optional.of(Marker.YOUR_TURN);
            case Busy busy -> Optional.empty();
            case Unknown unknown -> Optional.empty();
        };
    }

    /** Why the session waits on you, present only while it does. */
    default Optional<WaitingFor> waitingFor() {
        return switch (this) {
            case WaitingForPermission permission -> Optional.of(permission.reason());
            case WaitingForInput input -> Optional.of(input.reason());
            case Busy busy -> Optional.empty();
            case YourTurn yourTurn -> Optional.empty();
            case Unknown unknown -> Optional.empty();
        };
    }
}

package dev.maxenceng.lazyclaude.session.published;

import java.util.Collection;
import java.util.Optional;

/**
 * What a live session asks of you, weakest first so that declaration order is the ranking: your turn, waiting for input,
 * waiting for permission. Busy and unknown sessions ask nothing and carry none.
 */
public enum Marker {
    YOUR_TURN,
    WAITING_FOR_INPUT,
    WAITING_FOR_PERMISSION;

    public static Optional<Marker> strongest(Collection<Marker> markers) {
        return markers.stream().max(Marker::compareTo);
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.Unknown;

/**
 * How the hosted sessions stood at the last read; nothing before the first one. A session read as {@link Unknown} is
 * remembered as it last stood, since that read says nothing of whether it waits.
 */
public final class SeenStates {

    private final AtomicReference<Optional<Map<SessionId, SessionState>>> last = new AtomicReference<>(Optional.empty());

    /** Remembers {@code next} and answers what was remembered before it: empty on the first read. */
    public Optional<Map<SessionId, SessionState>> exchange(Map<SessionId, SessionState> next) {
        return last.getAndUpdate(previous -> Optional.of(carriedOver(next, previous.orElseGet(Map::of))));
    }

    private static Map<SessionId, SessionState> carriedOver(
            Map<SessionId, SessionState> next, Map<SessionId, SessionState> previous) {
        return next.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue() instanceof Unknown
                        ? previous.getOrDefault(entry.getKey(), entry.getValue())
                        : entry.getValue()));
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The notification each session has open on the desktop, by the id the desktop gave it. */
public final class OpenNotifications {

    private final AtomicReference<Map<SessionId, NotificationId>> shown = new AtomicReference<>(Map.of());

    public Optional<NotificationId> of(SessionId session) {
        return Optional.ofNullable(shown.get().get(session));
    }

    public List<SessionId> sessions() {
        return shown.get().keySet().stream().toList();
    }

    public void opened(SessionId session, NotificationId id) {
        shown.updateAndGet(open -> Stream.concat(
                        open.entrySet().stream().filter(entry -> !entry.getKey().equals(session)),
                        Stream.of(Map.entry(session, id)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /** Forgets the session's notification and answers it, if it had one open. */
    public Optional<NotificationId> closed(SessionId session) {
        Optional<NotificationId> was = of(session);
        shown.updateAndGet(open -> open.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(session))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
        return was;
    }
}

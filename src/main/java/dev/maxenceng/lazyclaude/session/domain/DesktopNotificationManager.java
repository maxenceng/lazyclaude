package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Decides, from how the machine's live sessions stood at the last read and stand now, which desktop notification to show,
 * replace or close. A session entering waiting is notified, unless it is the watched one; one moving between waiting for
 * permission and waiting for input has its notification replaced in place; one that leaves waiting, or dies, has it closed, and
 * so has one that becomes watched. The first read only records, so a session already waiting when lazyclaude starts is not
 * announced again, and a read that changes no state does nothing, however often the same states are handed over. A failure of
 * the desktop is named once, and the sessions go on.
 */
public final class DesktopNotificationManager {

    private static final String TITLE = "%s · %s";
    private static final String UNSAID_REASON = "waiting for you";
    private static final String WARNING = "notifications: %s.";

    private final DesktopNotificationPort notifications;
    private final WarningPort warnings;
    private final WarnOnce failing = new WarnOnce();
    private final SeenStates seen = new SeenStates();
    private final OpenNotifications open = new OpenNotifications();
    private final CurrentlyWatched watched = new CurrentlyWatched();

    public DesktopNotificationManager(DesktopNotificationPort notifications, WarningPort warnings) {
        this.notifications = notifications;
        this.warnings = warnings;
    }

    /** Takes the machine's live sessions as just read; sessions that could not be read change nothing. */
    public synchronized void seen(Project project, Sessions sessions) {
        if (!(sessions instanceof AvailableSessions available)) {
            return;
        }
        List<PlacedSession> now = available.everyone(project);
        Map<SessionId, SessionState> states = now.stream()
                .collect(Collectors.toUnmodifiableMap(placed -> placed.session().id(), placed -> placed.session().state()));
        seen.exchange(states).ifPresent(previous -> {
            open.sessions().stream().filter(id -> !states.containsKey(id)).forEach(this::closeFor);
            now.forEach(placed -> react(placed, Optional.ofNullable(previous.get(placed.session().id()))));
        });
    }

    /** From now on {@code next} is the session the user looks at: it is not notified, and any notification it has is closed. */
    public synchronized void watching(Watched next) {
        watched.watch(next);
        open.sessions().stream().filter(next::watches).forEach(this::closeFor);
    }

    private void react(PlacedSession placed, Optional<SessionState> was) {
        LiveSession session = placed.session();
        if (was.filter(session.state()::equals).isPresent()) {
            return;
        }
        Optional<WaitingFor> reason = reasonOf(session.state());
        if (reason.isEmpty()) {
            closeFor(session.id());
        } else if (was.flatMap(DesktopNotificationManager::reasonOf).isPresent()) {
            replaceFor(session.id(), notificationOf(placed, reason.get()));
        } else if (!watched.get().watches(session.id())) {
            fireFor(session.id(), notificationOf(placed, reason.get()));
        }
    }

    private void fireFor(SessionId session, DesktopNotification notification) {
        try {
            open.opened(session, notifications.show(notification));
        } catch (DesktopNotificationUnavailableException failure) {
            warn(failure);
        }
    }

    private void replaceFor(SessionId session, DesktopNotification notification) {
        try {
            open.of(session).ifPresent(id -> notifications.replace(id, notification));
        } catch (DesktopNotificationUnavailableException failure) {
            warn(failure);
        }
    }

    private void closeFor(SessionId session) {
        try {
            open.closed(session).ifPresent(notifications::close);
        } catch (DesktopNotificationUnavailableException failure) {
            warn(failure);
        }
    }

    private void warn(DesktopNotificationUnavailableException failure) {
        failing.raise(warnings, new Warning(WARNING.formatted(failure.getMessage())));
    }

    private static DesktopNotification notificationOf(PlacedSession placed, WaitingFor reason) {
        return DesktopNotification.builder()
                .title(new NotificationTitle(
                        TITLE.formatted(placed.place().name().value(), placed.session().name().value())))
                .body(new NotificationBody(
                        switch (reason) {
                            case WaitingReason said -> said.value();
                            case NoWaitingReason _ -> UNSAID_REASON;
                        }))
                .build();
    }

    /** Why the session waits on you, present only while it does. */
    private static Optional<WaitingFor> reasonOf(SessionState state) {
        return switch (state) {
            case WaitingForPermission permission -> Optional.of(permission.reason());
            case WaitingForInput input -> Optional.of(input.reason());
            case Busy _, YourTurn _, Unknown _ -> Optional.empty();
        };
    }
}

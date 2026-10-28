package dev.maxenceng.lazyclaude.session.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Decides, from how the live sessions lazyclaude hosts in the project stood at the last read and stand now, which desktop notification to show,
 * replace or close. A session entering waiting is notified, unless it is the watched one; one moving between waiting for
 * permission and waiting for input has its notification replaced in place; one that leaves waiting, or dies, has it closed, and
 * so has one that becomes watched. A session read as unknown keeps whatever notification it had. The first read only records, so a session already waiting when lazyclaude starts is not
 * announced again, and a read that changes no state does nothing, however often the same states are handed over. A failure of
 * the desktop is named once, and the sessions go on. With {@code desktop} off it never calls its port: markers and the status
 * line, which do not come through here, still show a waiting session.
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

    /** Takes the hosted sessions as just read; nothing changes while {@code desktop} is off. */
    public synchronized void seen(Project project, AvailableSessions sessions, boolean desktop) {
        if (!desktop) {
            return;
        }
        List<LiveSession> now = sessions.live();
        Map<SessionId, SessionState> states = now.stream()
                .collect(Collectors.toUnmodifiableMap(LiveSession::id, LiveSession::state));
        seen.exchange(states).ifPresent(previous -> {
            open.sessions().stream().filter(id -> !states.containsKey(id)).forEach(this::closeFor);
            now.stream()
                    .filter(session -> !(session.state() instanceof Unknown))
                    .forEach(session -> react(project, session, Optional.ofNullable(previous.get(session.id()))));
        });
    }

    /** From now on {@code next} is the session the user looks at: it is not notified, and any notification it has is closed. */
    public synchronized void watching(Watched next) {
        watched.watch(next);
        open.sessions().stream().filter(next::watches).forEach(this::closeFor);
    }

    private void react(Project project, LiveSession session, Optional<SessionState> was) {
        if (was.filter(session.state()::equals).isPresent()) {
            return;
        }
        session.state().waitingFor().ifPresentOrElse(
                reason -> notifyWaiting(project, session, reason, was), () -> closeFor(session.id()));
    }

    private void notifyWaiting(Project project, LiveSession session, WaitingFor reason, Optional<SessionState> was) {
        if (was.flatMap(SessionState::waitingFor).isPresent()) {
            replaceFor(session.id(), notificationOf(project, session, reason));
        } else if (!watched.get().watches(session.id())) {
            fireFor(session.id(), notificationOf(project, session, reason));
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
            open.of(session)
                    .map(id -> notifications.replace(id, notification))
                    .ifPresent(id -> open.opened(session, id));
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

    private static DesktopNotification notificationOf(Project project, LiveSession session, WaitingFor reason) {
        Path root = project.root();
        return DesktopNotification.builder()
                .title(new NotificationTitle(TITLE.formatted(
                        Optional.ofNullable(root.getFileName()).orElse(root), session.name().value())))
                .body(new NotificationBody(
                        switch (reason) {
                            case WaitingReason said -> said.value();
                            case NoWaitingReason _ -> UNSAID_REASON;
                        }))
                .build();
    }

}

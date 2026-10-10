package dev.maxenceng.lazyclaude.session.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionDirectory;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Builds the sessions of a project from every live session on the machine. It owns the rules: an
 * {@code sdk-cli} session is not listed, a session belongs to the project its {@code cwd} is located
 * in (and one whose {@code cwd} belongs to none is nobody's), and rows run newest first.
 */
public record SessionManager(
        LiveSessionPort sessions, ProjectLocatorPort locator, SessionWatchPort watch, SessionClock clock) {

    private static final Comparator<LiveSession> NEWEST_FIRST = Comparator.comparing(LiveSession::startedAt)
            .reversed()
            .thenComparing(session -> session.id().value());

    public Sessions sessionsFor(Project project) {
        List<LiveSession> live;
        try {
            live = sessions.liveSessions();
        } catch (SessionsUnavailableException unavailable) {
            return new UnavailableSessions(new Reason(unavailable.getMessage()));
        }
        List<LiveSession> candidates =
                live.stream().filter(session -> !session.isSdkCli()).toList();
        // deferred: git runs once per distinct cwd on every refresh, about every 2 s — close it when that shows up as load on a project with many worktrees.
        Map<SessionDirectory, Optional<Project>> located = new HashMap<>();
        candidates.forEach(session -> located.computeIfAbsent(session.directory(), this::locate));
        return LiveSessions.builder()
                .sessions(candidates.stream()
                        .filter(session -> located.get(session.directory()).filter(project::equals).isPresent())
                        .sorted(NEWEST_FIRST)
                        .toList())
                .clock(clock.frozen())
                .build();
    }

    public void watch(Runnable changed) {
        watch.watch(changed);
    }

    private Optional<Project> locate(SessionDirectory directory) {
        try {
            return Optional.of(locator.locate(new WorkingDirectory(directory.value())));
        } catch (ProjectNotFoundException outsideAnyProject) {
            return Optional.empty();
        }
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class SessionsFixture {

    private SessionsFixture() {}

    public static Clock clock() {
        return Clock.fixed(now(), ZoneOffset.UTC);
    }

    public static SessionClock sessionClock() {
        return new SessionClock(clock());
    }

    public static SessionClock sessionClockAt(Instant instant) {
        return new SessionClock(Clock.fixed(instant, ZoneOffset.UTC));
    }

    public static Clock advancingClock() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now(), now().plus(Duration.ofHours(1)), now().plus(Duration.ofHours(2)));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        return clock;
    }

    public static LiveSessions liveSessionsOf(List<LiveSession> sessions) {
        return LiveSessions.builder().sessions(sessions).clock(sessionClock()).build();
    }

    public static SessionsUnavailableException sessionsUnavailable() {
        return new SessionsUnavailableException(reason().value());
    }

    public static UnavailableSessions unavailableSessions() {
        return new UnavailableSessions(reason());
    }

    public static SessionManager sessionManager(LiveSessionPort sessions, ProjectLocatorPort locator, SessionWatchPort watch, WarningPort warnings) {
        return new SessionManager(sessions, locator, watch, warnings, sessionClock());
    }

    public static WorkingDirectory workingDirectoryOf(ReportedSession session) {
        return session.directory();
    }
}

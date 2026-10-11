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

    public static AvailableSessions liveSessionsOf(List<LiveSession> sessions) {
        return availableSessionsOf(sessions, List.of());
    }

    public static AvailableSessions pastSessionsOf(List<PastSession> past) {
        return availableSessionsOf(List.of(), past);
    }

    public static AvailableSessions availableSessionsOf(List<LiveSession> sessions, List<PastSession> past) {
        return AvailableSessions.builder().live(sessions).past(past).clock(sessionClock()).build();
    }

    public static AvailableSessions availableSessionsHosting(List<LiveSession> sessions, List<HostedTab> hosted) {
        return AvailableSessions.builder()
                .live(sessions)
                .past(List.of())
                .hosted(hosted)
                .clock(sessionClock())
                .build();
    }

    public static SessionsUnavailableException sessionsUnavailable() {
        return new SessionsUnavailableException(reason().value());
    }

    public static UnavailableSessions unavailableSessions() {
        return unavailableSessionsHosting(List.of());
    }

    public static UnavailableSessions unavailableSessionsHosting(List<HostedTab> hosted) {
        return UnavailableSessions.builder().reason(reason()).hosted(hosted).build();
    }

    public static SessionManager sessionManager(LiveSessionPort sessions, ProjectLocatorPort locator, SessionWatchPort watch, WarningPort warnings) {
        return sessionManager(sessions, mock(PastSessionPort.class), locator, watch, mock(TranscriptWatchPort.class), warnings);
    }

    public static SessionManager sessionManager(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings) {
        return sessionManager(sessions, past, locator, watch, transcriptWatch, warnings, noHostedSessions());
    }

    public static SessionManager sessionManager(
            LiveSessionPort sessions,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            WarningPort warnings,
            HostedSessionsSource hosted) {
        return sessionManager(
                sessions, mock(PastSessionPort.class), locator, watch, mock(TranscriptWatchPort.class), warnings, hosted);
    }

    public static SessionManager sessionManager(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostedSessionsSource hosted) {
        return new SessionManager(sessions, past, locator, watch, transcriptWatch, warnings, sessionClock(), hosted);
    }

    public static HostedSessionsSource noHostedSessions() {
        return () -> new HostedSessions(List.of());
    }

    public static WorkingDirectory workingDirectoryOf(ReportedSession session) {
        return session.directory();
    }
}

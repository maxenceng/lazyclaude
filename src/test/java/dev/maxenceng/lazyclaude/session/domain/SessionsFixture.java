package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.noTranscripts;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

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
        return availableSessionsHosting(past, sessions.stream().map(HostedSessionFixture::hostedLive).toList());
    }

    public static AvailableSessions availableSessionsHosting(List<PastSession> past, List<OpenHostedSession> hosted) {
        return AvailableSessions.builder().past(past).hosted(hosted).clock(sessionClock()).build();
    }

    public static AvailableSessions hostingSessions(List<OpenHostedSession> hosted) {
        return availableSessionsHosting(List.of(), hosted);
    }

    public static SessionManager sessionManager(LiveSessionPort sessions, LaunchLogPort log, SessionWatchPort watch, WarningPort warnings) {
        PastSessionPort past = mock(PastSessionPort.class);
        lenient().when(past.transcriptsOf(any())).thenReturn(noTranscripts());
        return sessionManager(sessions, past, log, watch, mock(TranscriptWatchPort.class), warnings);
    }

    public static SessionRecordUnreadableException recordUnreadable() {
        return new SessionRecordUnreadableException("%d.json does not parse: Unexpected end-of-input".formatted(processId().value()));
    }

    public static SessionManager sessionManager(
            LiveSessionPort sessions,
            PastSessionPort past,
            LaunchLogPort log,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings) {
        return new SessionManager(sessions, past, log, watch, transcriptWatch, warnings, clock());
    }
}

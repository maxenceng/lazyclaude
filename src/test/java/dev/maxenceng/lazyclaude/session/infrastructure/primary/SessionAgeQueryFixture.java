package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLogPort;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.quietPastSessions;
import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class SessionAgeQueryFixture {

    private SessionAgeQueryFixture() {}

    public static SessionAgeQuery sessionAgeQuery() {
        return new SessionAgeQuery(sessionApplicationService(
                mock(LiveSessionPort.class),
                quietPastSessions(),
                emptyLaunchLogPort(),
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                mock(WarningPort.class),
                mock(HostPort.class)));
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class SessionQueryFixture {

    private SessionQueryFixture() {}

    public static SessionQuery sessionQuery(LiveSessionPort sessions, ProjectLocatorPort locator, SessionWatchPort watch, WarningPort warnings) {
        return sessionQuery(sessions, mock(PastSessionPort.class), locator, watch, mock(TranscriptWatchPort.class), warnings);
    }

    public static SessionQuery sessionQuery(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings) {
        return new SessionQuery(new SessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, clock()));
    }
}

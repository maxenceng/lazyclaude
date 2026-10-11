package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;

import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class SessionApplicationServiceFixture {

    private SessionApplicationServiceFixture() {}

    public static SessionApplicationService sessionApplicationService(
            LiveSessionPort sessions,
            PastSessionPort past,
            ProjectLocatorPort locator,
            SessionWatchPort watch,
            TranscriptWatchPort transcriptWatch,
            WarningPort warnings,
            HostPort hosts) {
        return new SessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, clock());
    }
}

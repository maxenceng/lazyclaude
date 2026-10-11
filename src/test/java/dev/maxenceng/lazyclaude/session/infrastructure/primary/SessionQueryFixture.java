package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class SessionQueryFixture {

    private SessionQueryFixture() {}

    public static SessionQuery sessionQuery(LiveSessionPort sessions, ProjectLocatorPort locator, SessionWatchPort watch, WarningPort warnings) {
        return new SessionQuery(new SessionApplicationService(sessions, locator, watch, warnings, clock()));
    }
}

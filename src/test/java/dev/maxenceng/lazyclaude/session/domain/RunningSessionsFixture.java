package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;

import java.util.List;

public final class RunningSessionsFixture {

    private RunningSessionsFixture() {}

    public static RunningSessions runningSessions() {
        return RunningSessions.builder()
                .project(project())
                .ids(List.of(sessionId()))
                .live(List.of(liveSession()))
                .build();
    }
}

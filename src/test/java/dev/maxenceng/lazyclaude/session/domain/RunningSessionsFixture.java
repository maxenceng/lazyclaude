package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedLive;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;

import java.util.List;

public final class RunningSessionsFixture {

    private RunningSessionsFixture() {}

    public static RunningSessions runningSessions() {
        return RunningSessions.builder()
                .project(project())
                .hosted(List.of(hostedLive(liveSession())))
                .build();
    }
}

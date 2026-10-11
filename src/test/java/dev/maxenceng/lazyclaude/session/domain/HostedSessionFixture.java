package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.HostedSessionIdFixture.hostedSessionId;
import static dev.maxenceng.lazyclaude.session.published.HostedSessionIdFixture.secondHostedSessionId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import java.util.List;

import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;

public final class HostedSessionFixture {

    private HostedSessionFixture() {}

    public static HostedSession.HostedSessionBuilder hostedSessionBuilder() {
        return HostedSession.builder()
                .id(hostedSessionId())
                .pid(processId())
                .directory(projectRootDirectory());
    }

    public static HostedSession hostedSession() {
        return hostedSessionBuilder().build();
    }

    public static HostedSession secondHostedSession() {
        return hostedSessionBuilder()
                .id(secondHostedSessionId())
                .pid(secondProcessId())
                .build();
    }

    public static HostedSessionId idOf(HostStart start) {
        return ((StartedHost) start).id();
    }

    public static HostedSessions hostedSessionsOf(HostedSession... sessions) {
        return new HostedSessions(List.of(sessions));
    }

    public static HostedTab startingTab() {
        return HostedTab.builder().session(hostedSession()).phase(new StartingPhase()).build();
    }

    public static HostedTab registeredTab(LiveSession live) {
        return HostedTab.builder()
                .session(hostedSession())
                .phase(new RegisteredPhase(live))
                .build();
    }
}

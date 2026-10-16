package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.HostedSessionIdFixture.hostedSessionId;
import static dev.maxenceng.lazyclaude.session.published.HostedSessionIdFixture.secondHostedSessionId;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;

public final class HostedSessionFixture {

    private HostedSessionFixture() {}

    public static HostedSession.HostedSessionBuilder hostedSessionBuilder() {
        return HostedSession.builder()
                .id(hostedSessionId())
                .pid(processId())
                .directory(projectRootDirectory())
                .command(new NewClaude());
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

    public static TicketClaude ticketClaude(Verb verb) {
        return TicketClaude.builder().ticket(linkedTicketId()).verb(verb).build();
    }

    public static HostedSession hostedSessionRunning(Verb verb) {
        return hostedSessionBuilder().command(ticketClaude(verb)).build();
    }

    public static HostedSessionId idOf(HostStart start) {
        return ((StartedHost) start).id();
    }

    public static HostedSessions hostedSessionsOf(HostedSession... sessions) {
        return new HostedSessions(List.of(sessions));
    }

    public static OpenHostedSession openStarting() {
        return OpenHostedSession.builder().session(hostedSession()).phase(new StartingPhase()).build();
    }

    public static OpenHostedSession openUnread() {
        return OpenHostedSession.builder().session(hostedSession()).phase(new UnreadPhase()).build();
    }

    public static OpenHostedSession openLive(LiveSession live) {
        return OpenHostedSession.builder()
                .session(hostedSession())
                .phase(new LivePhase(live))
                .build();
    }
}

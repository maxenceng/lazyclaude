package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.earlierStartedAt;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class HostedSessionFixture {

    private HostedSessionFixture() {}

    public static HostedSession.HostedSessionBuilder hostedSessionBuilder() {
        return HostedSession.builder()
                .pid(processId())
                .directory(projectRootDirectory())
                .command(new NewClaude(sessionId()))
                .spawnedAt(earlierStartedAt());
    }

    public static HostedSession hostedSession() {
        return hostedSessionBuilder().build();
    }

    public static HostedSession secondHostedSession() {
        return hostedSessionBuilder()
                .pid(secondProcessId())
                .command(new NewClaude(secondSessionId()))
                .build();
    }

    public static TicketClaude ticketClaude(Verb verb) {
        return ticketClaude(sessionId(), verb);
    }

    public static TicketClaude ticketClaude(SessionId session, Verb verb) {
        return TicketClaude.builder().session(session).ticket(linkedTicketId()).verb(verb).build();
    }

    public static HostedSession hostedSessionRunning(Verb verb) {
        return hostedSessionBuilder().command(ticketClaude(verb)).build();
    }

    public static SessionId idOf(HostStart start) {
        return ((StartedHost) start).id();
    }

    public static HostedSessions hostedSessionsOf(HostedSession... sessions) {
        return new HostedSessions(List.of(sessions));
    }

    public static HostedSessions noneHosted() {
        return hostedSessionsOf();
    }

    public static HostedSessions oneHosted() {
        return hostedSessionsOf(hostedSession());
    }

    public static HostedSessions twoHosted() {
        return hostedSessionsOf(hostedSession(), secondHostedSession());
    }

    public static OpenHostedSession openStarting() {
        return OpenHostedSession.builder().session(hostedSession()).phase(new StartingPhase()).build();
    }

    public static OpenHostedSession openUnread() {
        return OpenHostedSession.builder().session(hostedSession()).phase(new UnreadPhase(unknownSession())).build();
    }

    public static LiveSession unknownSession() {
        return LiveSessionFixture.liveSessionBuilder()
                .name(hostedSession().name())
                .startedAt(earlierStartedAt())
                .version(new NoVersion())
                .state(new Unknown())
                .build();
    }

    public static OpenHostedSession hostedLive(LiveSession live) {
        return OpenHostedSession.builder()
                .session(HostedSession.builder()
                        .pid(live.pid())
                        .directory(live.directory())
                        .command(new NewClaude(live.id()))
                        .spawnedAt(earlierStartedAt())
                        .build())
                .phase(new LivePhase(live))
                .build();
    }

    public static OpenHostedSession openLive(LiveSession live) {
        return OpenHostedSession.builder()
                .session(hostedSession())
                .phase(new LivePhase(live))
                .build();
    }

    public static OpenHostedSession startingIn(WorkingDirectory directory) {
        return OpenHostedSession.builder()
                .session(hostedSessionBuilder().directory(directory).build())
                .phase(new StartingPhase())
                .build();
    }
}

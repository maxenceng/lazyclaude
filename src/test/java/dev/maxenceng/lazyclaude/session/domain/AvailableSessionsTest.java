package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openUnread;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.startingIn;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionIn;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.strongLink;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.weakLink;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectoryAt;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.Unknown;

class AvailableSessionsTest {

    @Test
    void shouldListTheLiveSessionsOfTheHostedOnesNewestStartFirst() {
        AvailableSessions sessions = liveSessionsOf(List.of(liveSession(), newestSession()));

        assertThat(sessions.live()).containsExactly(newestSession(), liveSession());
    }

    @Test
    void shouldListNoLiveSessionForAHostedOneStillStarting() {
        assertThat(availableSessionsHosting(List.of(), List.of(openStarting())).live()).isEmpty();
    }

    @Test
    void shouldListAnUnreadHostedSessionAsLiveWithAnUnknownState() {
        assertThat(availableSessionsHosting(List.of(), List.of(openUnread())).live())
                .extracting(LiveSession::state)
                .containsExactly(new Unknown());
    }

    @Test
    void shouldHaveRunAVerbWhenAPastSessionLastRanItOnTheTicket() {
        PastSession design = pastSessionBuilder().links(List.of(strongLink("PROJ-1", "design"))).build();

        assertThat(pastSessionsOf(List.of(design)).ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldHaveRunAVerbWhenALiveSessionLastRanItOnTheTicket() {
        LiveSession design = liveSessionBuilder().links(List.of(strongLink("PROJ-1", "design"))).build();

        assertThat(liveSessionsOf(List.of(design)).ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldHaveRunAVerbWhenAHostedSessionStillStartingWasLaunchedWithIt() {
        AvailableSessions sessions = availableSessionsHosting(
                List.of(),
                List.of(OpenHostedSession.builder()
                        .session(hostedSessionRunning(Verb.DESIGN))
                        .phase(new StartingPhase())
                        .build()));

        assertThat(sessions.ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldNotHaveRunAVerbAnotherTicketsSessionRan() {
        PastSession design = pastSessionBuilder().links(List.of(strongLink("PROJ-2", "design"))).build();

        assertThat(pastSessionsOf(List.of(design)).ranVerb(linkedTicketId(), Verb.DESIGN)).isFalse();
    }

    @Test
    void shouldNotHaveRunAVerbAWeakLinkNamesNoVerbFor() {
        PastSession weak = pastSessionBuilder().links(List.of(weakLink("PROJ-1"))).build();

        assertThat(pastSessionsOf(List.of(weak)).ranVerb(linkedTicketId(), Verb.DESIGN)).isFalse();
    }

    @Test
    void shouldNameTheLiveSessionsWhoseDirectoryIsBelowTheWorktree() {
        AvailableSessions sessions = liveSessionsOf(List.of(
                sessionIn(workingDirectoryAt("/work/app.worktrees/PROJ-1-ticket/src/main")),
                newestSessionBuilder().directory(projectRootDirectory()).build()));

        assertThat(sessions.namesIn(worktreeDirectory())).containsExactly(sessionName());
    }

    @Test
    void shouldNameTheLiveSessionWhoseDirectoryIsTheWorktreeItself() {
        assertThat(liveSessionsOf(List.of(sessionIn(worktreeDirectory()))).namesIn(worktreeDirectory()))
                .containsExactly(sessionName());
    }

    @Test
    void shouldNotNameALiveSessionInASiblingWhoseNameBeginsWithTheWorktreesName() {
        AvailableSessions sessions =
                liveSessionsOf(List.of(sessionIn(workingDirectoryAt("/work/app.worktrees/PROJ-1-ticket-again"))));

        assertThat(sessions.namesIn(worktreeDirectory())).isEmpty();
    }

    @Test
    void shouldNameAHostedSessionStillStartingInTheWorktree() {
        AvailableSessions sessions = availableSessionsHosting(
                List.of(), List.of(startingIn(worktreeDirectory())));

        assertThat(sessions.namesIn(worktreeDirectory()))
                .as("a claude with no registry record yet is already running in the directory")
                .containsExactly(new SessionName("claude"));
    }

    @Test
    void shouldNameAHostedSessionOnceWhenItIsLive() {
        AvailableSessions sessions = availableSessionsHosting(
                List.of(),
                List.of(OpenHostedSession.builder()
                        .session(hostedSessionBuilder().directory(worktreeDirectory()).build())
                        .phase(new LivePhase(sessionIn(worktreeDirectory())))
                        .build()));

        assertThat(sessions.namesIn(worktreeDirectory())).containsExactly(sessionName());
    }
}

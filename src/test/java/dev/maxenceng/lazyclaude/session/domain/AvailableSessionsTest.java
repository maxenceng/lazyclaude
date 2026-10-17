package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.ElsewhereGroupFixture.otherProjectGroup;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openLive;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.startingIn;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.hostedLiveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionIn;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsWithElsewhere;
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

class AvailableSessionsTest {

    @Test
    void shouldMarkALiveSessionHostedWhenItsPidIsOneLazyclaudeSpawned() {
        Sessions hosting = liveSessionsOf(List.of(liveSession())).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting).isEqualTo(availableSessionsHosting(List.of(hostedLiveSession()), List.of(openLive(hostedLiveSession()))));
    }

    @Test
    void shouldLeaveALiveSessionOutsideWhenItsPidIsNotOneLazyclaudeSpawned() {
        AvailableSessions hosting = liveSessionsOf(List.of(newestSession())).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting.live()).extracting(LiveSession::hosting).containsExactly(Hosting.OUTSIDE);
    }

    @Test
    void shouldMarkASessionElsewhereHostedWhenItsPidIsOneLazyclaudeSpawned() {
        AvailableSessions hosting = liveSessionsWithElsewhere(List.of(), List.of(otherProjectGroup(List.of(liveSession()))))
                .hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting.elsewhere())
                .as("a session lazyclaude hosts is never an outside session, wherever its cwd belongs")
                .isEqualTo(List.of(otherProjectGroup(List.of(hostedLiveSession()))));
    }

    @Test
    void shouldLeaveASessionElsewhereOutsideWhenItsPidIsNotOneLazyclaudeSpawned() {
        AvailableSessions hosting = liveSessionsWithElsewhere(List.of(), List.of(otherProjectGroup(List.of(newestSession()))))
                .hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting.elsewhere()).isEqualTo(List.of(otherProjectGroup(List.of(newestSession()))));
    }

    @Test
    void shouldFindALiveSessionOfTheProjectById() {
        assertThat(liveSessionsOf(List.of(liveSession(), newestSession())).liveSession(newestSession().id())).contains(newestSession());
    }

    @Test
    void shouldFindALiveSessionElsewhereById() {
        AvailableSessions sessions =
                liveSessionsWithElsewhere(List.of(liveSession()), List.of(otherProjectGroup(List.of(newestSession()))));

        assertThat(sessions.liveSession(newestSession().id())).contains(newestSession());
    }

    @Test
    void shouldFindNoLiveSessionForAnIdNoneRuns() {
        assertThat(liveSessionsOf(List.of(liveSession())).liveSession(newestSession().id())).isEmpty();
    }

    @Test
    void shouldListAHostedSessionAsStartingWhileTheRegistryHasNoRecordOfIt() {
        Sessions hosting = liveSessionsOf(List.of()).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting).isEqualTo(availableSessionsHosting(List.of(), List.of(openStarting())));
    }

    @Test
    void shouldLinkALiveHostedSessionToTheTicketItWasStartedFor() {
        HostedSession started = hostedSessionRunning(Verb.REVIEW);
        AvailableSessions hosting =
                liveSessionsOf(List.of(liveSession())).hosting(hostedSessionsOf(started));

        assertThat(hosting.live())
                .extracting(LiveSession::links)
                .as("its transcript has not linked it yet, and it must not drop to the unlinked section meanwhile")
                .containsExactly(List.of(strongLink("PROJ-1", "review")));
    }

    @Test
    void shouldKeepTheLinkItsTranscriptAlreadyGivesToThatTicket() {
        LiveSession linked = liveSessionBuilder().links(List.of(strongLink("PROJ-1", "respond"))).build();

        AvailableSessions hosting = liveSessionsOf(List.of(linked)).hosting(hostedSessionsOf(hostedSessionRunning(Verb.REVIEW)));

        assertThat(hosting.live()).extracting(LiveSession::links).containsExactly(List.of(strongLink("PROJ-1", "respond")));
    }

    @Test
    void shouldLinkNoLiveSessionThatIsNotHostedForATicket() {
        AvailableSessions hosting = liveSessionsOf(List.of(liveSession())).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting.live()).extracting(LiveSession::links).containsExactly(List.of());
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
        AvailableSessions hosting = liveSessionsOf(List.of()).hosting(hostedSessionsOf(hostedSessionRunning(Verb.DESIGN)));

        assertThat(hosting.ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
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
    void shouldNameASessionElsewhereWhoseDirectoryIsInTheWorktree() {
        AvailableSessions sessions = liveSessionsWithElsewhere(
                List.of(), List.of(otherProjectGroup(List.of(sessionIn(worktreeDirectory())))));

        assertThat(sessions.namesIn(worktreeDirectory()))
                .as("any live session counts, whichever place the sessions are grouped under")
                .containsExactly(sessionName());
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
                List.of(sessionIn(worktreeDirectory())),
                List.of(OpenHostedSession.builder()
                        .session(hostedSessionBuilder().directory(worktreeDirectory()).build())
                        .phase(new LivePhase(sessionIn(worktreeDirectory())))
                        .build()));

        assertThat(sessions.namesIn(worktreeDirectory())).containsExactly(sessionName());
    }
}

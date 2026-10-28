package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openLive;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.weak;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedPastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.liveHostedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.startingHostedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.startingTicketHostedSessionView;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.session.published.TranscriptReadingFixture.unreadable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.OpenHostedSession;
import dev.maxenceng.lazyclaude.session.domain.StartingPhase;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class SessionsViewTest {

    @Test
    void shouldCountEachMarkerAmongEveryLiveSessionLinkedOrNot() {
        SessionsView view = sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new YourTurn()),
                linkedSessionView(sessionIdOf(2), waitingForPermission(), strong("PROJ-1", "start")),
                linkedSessionView(sessionIdOf(3), waitingForPermission())));

        assertThat(view.markerCounts())
                .containsOnly(entry(Marker.WAITING_FOR_PERMISSION, 2L), entry(Marker.YOUR_TURN, 1L));
    }

    @Test
    void shouldCountNoMarkerWhenEverySessionIsBusy() {
        SessionsView view = sessionsViewOf(List.of(linkedSessionView(sessionIdOf(1), new Busy())));

        assertThat(view.markerCounts()).isEmpty();
    }

    @Test
    void shouldAnswerTheStrongestMarkerAmongTheLiveSessionsLinkedToATicketWeakLinksIncluded() {
        SessionsView view = sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), waitingForPermission(), strong("PROJ-2", "start")),
                linkedSessionView(sessionIdOf(2), new YourTurn(), strong("PROJ-1", "start")),
                linkedSessionView(sessionIdOf(3), waitingForInput(), weak("PROJ-1"))));

        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-1"))).contains(Marker.WAITING_FOR_INPUT);
        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-2"))).contains(Marker.WAITING_FOR_PERMISSION);
    }

    @Test
    void shouldAnswerNoMarkerForATicketWhoseSessionsAreAllBusyOrHaveNone() {
        SessionsView view = sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new Busy(), strong("PROJ-1", "start")),
                linkedSessionView(sessionIdOf(2), new YourTurn())));

        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-1"))).isEmpty();
        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-3"))).isEmpty();
    }

    @Test
    void shouldAnswerTheTicketsSessionWithTheStrongestMarker() {
        LiveSessionView strongest = linkedSessionView(sessionIdOf(2), waitingForPermission(), weak("PROJ-1"));
        SessionsView view = sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new YourTurn(), strong("PROJ-1", "start")),
                strongest,
                linkedSessionView(sessionIdOf(3), waitingForPermission(), strong("PROJ-2", "start"))));

        assertThat(view.strongestSessionOf(linkedTicketId("PROJ-1"))).contains(strongest);
    }

    @Test
    void shouldAnswerTheFirstOfTheTicketsSessionsTiedOnTheStrongestMarker() {
        LiveSessionView first = linkedSessionView(sessionIdOf(1), waitingForInput(), strong("PROJ-1", "start"));
        SessionsView view = sessionsViewOf(List.of(
                first, linkedSessionView(sessionIdOf(2), waitingForInput(), strong("PROJ-1", "review"))));

        assertThat(view.strongestSessionOf(linkedTicketId("PROJ-1"))).as("the Sessions panel lists them in this order, so enter lands on the top one").contains(first);
    }

    @Test
    void shouldAnswerNoSessionForATicketWhoseSessionsCarryNoMarker() {
        SessionsView view = sessionsViewOf(List.of(linkedSessionView(sessionIdOf(1), new Busy(), strong("PROJ-1", "start"))));

        assertThat(view.strongestSessionOf(linkedTicketId("PROJ-1"))).isEmpty();
    }

    @Test
    void shouldAnswerOnlyTheSessionsLinkedToNoTicketAsUnlinked() {
        LiveSessionView unlinkedLive = linkedSessionView(sessionIdOf(1), new YourTurn());
        PastSessionView unlinkedPast = linkedPastSessionView(1);
        SessionsView view = sessionsViewOf(
                List.of(unlinkedLive, linkedSessionView(sessionIdOf(2), new YourTurn(), weak("PROJ-1"))),
                List.of(linkedPastSessionView(2, strong("PROJ-1", "start")), unlinkedPast));

        assertThat(view.unlinkedLive()).containsExactly(unlinkedLive);
        assertThat(view.unlinkedPast()).containsExactly(unlinkedPast);
    }

    @Test
    void shouldViewAStartingHostedSessionWithNoLiveSessionBehindIt() {
        SessionsView view = SessionsView.from(availableSessionsHosting(List.of(), List.of(openStarting())));

        assertThat(view.hosted()).containsExactly(startingHostedSessionView());
    }

    @Test
    void shouldViewAStartingHostedSessionWithTheTicketItWasStartedFor() {
        SessionsView view = SessionsView.from(availableSessionsHosting(
                List.of(), List.of(OpenHostedSession.builder().session(hostedSessionRunning(Verb.REVIEW)).phase(new StartingPhase()).build())));

        assertThat(view.hosted()).containsExactly(startingTicketHostedSessionView());
    }

    @Test
    void shouldViewARegisteredHostedSessionWithTheLiveSessionItIs() {
        SessionsView view = SessionsView.from(
                availableSessionsHosting(List.of(), List.of(openLive(liveSession()))));

        assertThat(view.hosted()).containsExactly(liveHostedSessionView(sessionView(), true));
        assertThat(view.live()).containsExactly(sessionView());
    }


    @Test
    void shouldViewAPastSessionWithTheTitleAndOpeningPromptItsTranscriptSays() {
        SessionsView view = SessionsView.from(pastSessionsOf(List.of(pastSession())));

        assertThat(view.past()).containsExactly(pastSessionView());
    }

    @Test
    void shouldViewAPastSessionWhoseTranscriptCannotBeReadWithTheReason() {
        SessionsView view =
                SessionsView.from(pastSessionsOf(List.of(pastSessionBuilder().reading(unreadable()).build())));

        assertThat(view.past()).containsExactly(pastSessionViewBuilder().reading(unreadable()).build());
    }
}

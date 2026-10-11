package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.weak;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedPastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class AvailableSessionsViewTest {

    @Test
    void shouldAnswerTheStrongestMarkerAmongEveryLiveSessionLinkedOrNot() {
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new YourTurn()),
                linkedSessionView(sessionIdOf(2), new WaitingForPermission(), strong("PROJ-1", "start"))));

        assertThat(view.strongestMarker()).contains(Marker.WAITING_FOR_PERMISSION);
    }

    @Test
    void shouldAnswerNoMarkerWhenEverySessionIsBusy() {
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(linkedSessionView(sessionIdOf(1), new Busy())));

        assertThat(view.strongestMarker()).isEmpty();
    }

    @Test
    void shouldAnswerTheStrongestMarkerAmongTheLiveSessionsLinkedToATicketWeakLinksIncluded() {
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new WaitingForPermission(), strong("PROJ-2", "start")),
                linkedSessionView(sessionIdOf(2), new YourTurn(), strong("PROJ-1", "start")),
                linkedSessionView(sessionIdOf(3), waitingForInput(), weak("PROJ-1"))));

        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-1"))).contains(Marker.WAITING_FOR_INPUT);
        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-2"))).contains(Marker.WAITING_FOR_PERMISSION);
    }

    @Test
    void shouldAnswerNoMarkerForATicketWhoseSessionsAreAllBusyOrHaveNone() {
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new Busy(), strong("PROJ-1", "start")),
                linkedSessionView(sessionIdOf(2), new YourTurn())));

        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-1"))).isEmpty();
        assertThat(view.strongestMarkerOf(linkedTicketId("PROJ-3"))).isEmpty();
    }

    @Test
    void shouldAnswerTheTicketsSessionWithTheStrongestMarker() {
        LiveSessionView strongest = linkedSessionView(sessionIdOf(2), new WaitingForPermission(), weak("PROJ-1"));
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new YourTurn(), strong("PROJ-1", "start")),
                strongest,
                linkedSessionView(sessionIdOf(3), new WaitingForPermission(), strong("PROJ-2", "start"))));

        assertThat(view.strongestSessionOf(linkedTicketId("PROJ-1"))).contains(strongest);
    }

    @Test
    void shouldAnswerTheFirstOfTheTicketsSessionsTiedOnTheStrongestMarker() {
        LiveSessionView first = linkedSessionView(sessionIdOf(1), waitingForInput(), strong("PROJ-1", "start"));
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(
                first, linkedSessionView(sessionIdOf(2), waitingForInput(), strong("PROJ-1", "review"))));

        assertThat(view.strongestSessionOf(linkedTicketId("PROJ-1"))).as("the Sessions panel lists them in this order, so enter lands on the top one").contains(first);
    }

    @Test
    void shouldAnswerNoSessionForATicketWhoseSessionsCarryNoMarker() {
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(linkedSessionView(sessionIdOf(1), new Busy(), strong("PROJ-1", "start"))));

        assertThat(view.strongestSessionOf(linkedTicketId("PROJ-1"))).isEmpty();
    }

    @Test
    void shouldAnswerOnlyTheSessionsLinkedToNoTicketAsUnlinked() {
        LiveSessionView unlinkedLive = linkedSessionView(sessionIdOf(1), new YourTurn());
        PastSessionView unlinkedPast = linkedPastSessionView(1);
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(
                List.of(unlinkedLive, linkedSessionView(sessionIdOf(2), new YourTurn(), weak("PROJ-1"))),
                List.of(linkedPastSessionView(2, strong("PROJ-1", "start")), unlinkedPast));

        assertThat(view.unlinkedLive()).containsExactly(unlinkedLive);
        assertThat(view.unlinkedPast()).containsExactly(unlinkedPast);
    }
}

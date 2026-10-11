package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.weak;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
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

        assertThat(view.strongestMarkerOf("PROJ-1")).contains(Marker.WAITING_FOR_INPUT);
        assertThat(view.strongestMarkerOf("PROJ-2")).contains(Marker.WAITING_FOR_PERMISSION);
    }

    @Test
    void shouldAnswerNoMarkerForATicketWhoseSessionsAreAllBusyOrHaveNone() {
        AvailableSessionsView view = (AvailableSessionsView) sessionsViewOf(List.of(
                linkedSessionView(sessionIdOf(1), new Busy(), strong("PROJ-1", "start")),
                linkedSessionView(sessionIdOf(2), new YourTurn())));

        assertThat(view.strongestMarkerOf("PROJ-1")).isEmpty();
        assertThat(view.strongestMarkerOf("PROJ-3")).isEmpty();
    }
}

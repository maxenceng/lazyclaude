package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.elsewhereGroupViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionViewWith;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

class ElsewhereGroupViewTest {

    @Test
    void shouldAnswerTheStrongestWaitingMarkerAmongItsSessions() {
        ElsewhereGroupView group = elsewhereGroupViewOf(List.of(
                sessionViewWith(new YourTurn()), sessionViewWith(waitingForInput()), sessionViewWith(new Busy())));

        assertThat(group.strongestWaiting()).contains(Marker.WAITING_FOR_INPUT);
    }

    @Test
    void shouldAnswerPermissionOverInput() {
        ElsewhereGroupView group =
                elsewhereGroupViewOf(List.of(sessionViewWith(waitingForInput()), sessionViewWith(waitingForPermission())));

        assertThat(group.strongestWaiting()).contains(Marker.WAITING_FOR_PERMISSION);
    }

    @Test
    void shouldAnswerNothingWhenNoSessionWaitsOnYou() {
        ElsewhereGroupView group = elsewhereGroupViewOf(List.of(sessionViewWith(new YourTurn()), sessionViewWith(new Busy())));

        assertThat(group.strongestWaiting()).as("a project that is at most on your turn is not listed on the status line").isEmpty();
    }
}

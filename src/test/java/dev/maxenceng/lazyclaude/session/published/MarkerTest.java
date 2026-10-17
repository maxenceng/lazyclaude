package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MarkerTest {

    @Test
    void shouldMarkAStateWaitingForPermissionAsTheStrongest() {
        assertThat(waitingForPermission().marker()).contains(Marker.WAITING_FOR_PERMISSION);
    }

    @Test
    void shouldMarkAStateWaitingForInput() {
        assertThat(waitingForInput().marker()).contains(Marker.WAITING_FOR_INPUT);
    }

    @Test
    void shouldMarkYourTurn() {
        assertThat(new YourTurn().marker()).contains(Marker.YOUR_TURN);
    }

    @Test
    void shouldMarkNothingForBusyOrUnknown() {
        assertThat(new Busy().marker()).as("a working session asks nothing of you").isEmpty();
        assertThat(new Unknown().marker()).as("a state lazyclaude does not know cannot be ranked").isEmpty();
    }

    @Test
    void shouldRankPermissionAboveInputAboveYourTurn() {
        assertThat(Marker.strongest(List.of(Marker.YOUR_TURN, Marker.WAITING_FOR_PERMISSION, Marker.WAITING_FOR_INPUT)))
                .contains(Marker.WAITING_FOR_PERMISSION);
        assertThat(Marker.strongest(List.of(Marker.YOUR_TURN, Marker.WAITING_FOR_INPUT)))
                .contains(Marker.WAITING_FOR_INPUT);
    }

    @Test
    void shouldHaveNoStrongestMarkerWithoutAny() {
        assertThat(Marker.strongest(List.of())).isEqualTo(Optional.empty());
    }
}

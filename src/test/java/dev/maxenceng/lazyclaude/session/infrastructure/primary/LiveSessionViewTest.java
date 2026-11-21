package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionViewWith;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.Unknown;

class LiveSessionViewTest {

    @Test
    void shouldCarryTheMarkerItsStateCarries() {
        assertThat(sessionViewWith(waitingForPermission()).marker())
                .contains(MarkerView.of(Marker.WAITING_FOR_PERMISSION));
    }

    @Test
    void shouldCarryNoMarkerWhenBusyOrUnknown() {
        assertThat(sessionViewWith(new Busy()).marker()).as("a working session asks nothing of you").isEmpty();
        assertThat(sessionViewWith(new Unknown()).marker()).as("a state lazyclaude does not know cannot be ranked").isEmpty();
    }

    @Test
    void shouldRefuseAMarkerItsStateDoesNotCarry() {
        LiveSessionView busy = sessionViewWith(new Busy());

        assertThatThrownBy(() -> new LiveSessionView(
                        busy.id(),
                        busy.pid(),
                        busy.name(),
                        busy.directory(),
                        busy.startedAt(),
                        busy.version(),
                        busy.state(),
                        Optional.of(MarkerView.of(Marker.YOUR_TURN)),
                        busy.links()))
                .as("the marker is derived from the state, never set beside it")
                .isInstanceOf(MarkerBesideStateException.class);
    }
}

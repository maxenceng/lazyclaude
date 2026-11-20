package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Marker;

class MarkerViewTest {

    @Test
    void shouldGiveAStrongerMarkerAGreaterStrength() {
        List<Marker> markers = Arrays.asList(Marker.values());

        assertThat(markers.stream()
                        .flatMap(stronger -> markers.stream()
                                .filter(weaker -> stronger.compareTo(weaker) > 0)
                                .filter(weaker -> MarkerView.of(stronger).strength() <= MarkerView.of(weaker).strength())
                                .map(weaker -> stronger + " over " + weaker))
                        .toList())
                .as("the frontend orders sessions by strength alone, so it has to rank markers as Marker's declaration does")
                .isEmpty();
    }

    @Test
    void shouldRankWaitingForPermissionOverWaitingForInputOverYourTurn() {
        assertThat(MarkerView.of(Marker.WAITING_FOR_PERMISSION).strength())
                .isGreaterThan(MarkerView.of(Marker.WAITING_FOR_INPUT).strength());
        assertThat(MarkerView.of(Marker.WAITING_FOR_INPUT).strength())
                .isGreaterThan(MarkerView.of(Marker.YOUR_TURN).strength());
    }

    @Test
    void shouldCarryTheMarkerItRanks() {
        assertThat(MarkerView.of(Marker.WAITING_FOR_INPUT).kind()).isEqualTo(Marker.WAITING_FOR_INPUT);
    }

    @Test
    void shouldRefuseAStrengthOtherThanItsMarkersRank() {
        int rankOfWaitingForPermission = Marker.WAITING_FOR_PERMISSION.ordinal();

        assertThatThrownBy(() -> new MarkerView(Marker.YOUR_TURN, rankOfWaitingForPermission))
                .as("a strength set beside the kind would let the frontends order your turn above a permission prompt")
                .isInstanceOf(MarkerStrengthNotItsRankException.class);
    }
}

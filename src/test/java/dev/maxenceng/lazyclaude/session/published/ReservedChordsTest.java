package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altJ;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altK;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altL;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altM;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altW;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altZ;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlAltH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlC;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.pressed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ReservedChordsTest {

    @Test
    void shouldDefaultToTheChordsOfTheLayoutDecision() {
        ReservedChords defaults = ReservedChords.defaults();

        assertThat(List.of(
                        defaults.of(ReservedAction.FOCUS_LEFT),
                        defaults.of(ReservedAction.FOCUS_DOWN),
                        defaults.of(ReservedAction.FOCUS_UP),
                        defaults.of(ReservedAction.FOCUS_RIGHT),
                        defaults.of(ReservedAction.HIDE_MAIN_VIEW),
                        defaults.of(ReservedAction.ZOOM),
                        defaults.of(ReservedAction.NEXT_WAITING)))
                .containsExactly(altH(), altJ(), altK(), altL(), altM(), altZ(), altW());
    }

    @Test
    void shouldRebindOneActionAndLeaveTheOthersAtTheirDefault() {
        ReservedChords rebound = ReservedChords.defaults().rebinding(Map.of(ReservedAction.FOCUS_LEFT, ctrlAltH()));

        assertThat(rebound.of(ReservedAction.FOCUS_LEFT)).isEqualTo(ctrlAltH());
        assertThat(rebound.of(ReservedAction.FOCUS_DOWN)).isEqualTo(altJ());
        assertThat(rebound.of(ReservedAction.NEXT_WAITING)).isEqualTo(altW());
    }

    @Test
    void shouldRefuseTwoActionsOnOneChordNamingBoth() {
        assertThatThrownBy(() -> ReservedChords.defaults().rebinding(Map.of(ReservedAction.FOCUS_LEFT, altJ())))
                .isInstanceOfSatisfying(DuplicateChordException.class, refused -> {
                    assertThat(refused.chord()).isEqualTo(altJ());
                    assertThat(List.of(refused.first(), refused.second()))
                            .containsExactlyInAnyOrder(ReservedAction.FOCUS_LEFT, ReservedAction.FOCUS_DOWN);
                });
    }

    @Test
    void shouldRefuseAnActionOnAFixedChordNamingBoth() {
        assertThatThrownBy(() -> ReservedChords.defaults().rebinding(Map.of(ReservedAction.ZOOM, ctrlC())))
                .isInstanceOfSatisfying(TakenChordException.class, refused -> {
                    assertThat(refused.action()).isEqualTo(ReservedAction.ZOOM);
                    assertThat(refused.fixed()).isEqualTo(FixedChord.QUIT);
                });
    }

    @Test
    void shouldAllowTwoActionsToSwapChords() {
        ReservedChords swapped = ReservedChords.defaults()
                .rebinding(Map.of(ReservedAction.FOCUS_LEFT, altJ(), ReservedAction.FOCUS_DOWN, altH()));

        assertThat(swapped.of(ReservedAction.FOCUS_LEFT)).isEqualTo(altJ());
        assertThat(swapped.of(ReservedAction.FOCUS_DOWN)).isEqualTo(altH());
    }

    @Test
    void shouldNameTheActionAPressedKeyIsBoundTo() {
        ReservedChords rebound = ReservedChords.defaults().rebinding(Map.of(ReservedAction.FOCUS_LEFT, ctrlAltH()));

        assertThat(rebound.actionOf(pressed('h', Modifier.CTRL, Modifier.ALT)))
                .contains(ReservedAction.FOCUS_LEFT);
        assertThat(rebound.actionOf(pressed('h', Modifier.ALT)))
                .as("the old chord is free again, so it reaches claude")
                .isEqualTo(Optional.empty());
    }

    @Test
    void shouldNameTheReservedActionsAClaudeBindingShadows() {
        assertThat(ReservedChords.defaults().clashingWith(List.of(altZ(), ctrlAltH(), altM())))
                .containsExactly(ReservedAction.HIDE_MAIN_VIEW, ReservedAction.ZOOM);
    }

    @Test
    void shouldFindNoClashWhenNothingIsBoundOnAReservedChord() {
        assertThat(ReservedChords.defaults().clashingWith(List.of(ctrlAltH()))).isEmpty();
    }
}

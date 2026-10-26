package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.chord;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlAltH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.namedChord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.InvalidChordException;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.NamedKey;

class ChordSpellingTest {

    @Test
    void shouldReadAChordWithOneModifier() {
        assertThat(ChordSpelling.configured("alt+h")).isEqualTo(altH());
    }

    @Test
    void shouldReadAChordWithTwoModifiersInEitherOrder() {
        assertThat(ChordSpelling.configured("alt+ctrl+h")).isEqualTo(ctrlAltH());
    }

    @Test
    void shouldReadANamedKey() {
        assertThat(ChordSpelling.configured("ctrl+PageUp")).isEqualTo(namedChord(NamedKey.PAGE_UP, Modifier.CTRL));
    }

    @Test
    void shouldRefuseABareKey() {
        assertThatThrownBy(() -> ChordSpelling.configured("h")).isInstanceOf(InvalidChordException.class).hasMessageContaining("no modifier");
    }

    @Test
    void shouldRefuseShiftAlone() {
        assertThatThrownBy(() -> ChordSpelling.configured("shift+h")).isInstanceOf(InvalidChordException.class).hasMessageContaining("shift alone");
    }

    @Test
    void shouldAcceptShiftWithCtrl() {
        assertThat(ChordSpelling.configured("ctrl+shift+h")).isEqualTo(chord('h', Modifier.CTRL, Modifier.SHIFT));
    }

    @Test
    void shouldRefuseAModifierThatIsNotCtrlAltOrShift() {
        assertThatThrownBy(() -> ChordSpelling.configured("meta+h"))
                .isInstanceOf(InvalidChordException.class)
                .hasMessageContaining("'meta' is not a modifier");
        assertThatThrownBy(() -> ChordSpelling.configured("cmd+h")).isInstanceOf(InvalidChordException.class);
    }

    @Test
    void shouldRefuseASequenceOfKeystrokes() {
        assertThatThrownBy(() -> ChordSpelling.configured("ctrl+x ctrl+k"))
                .isInstanceOf(InvalidChordException.class)
                .hasMessageContaining("several keystrokes");
    }

    @Test
    void shouldRefuseAKeyItDoesNotKnow() {
        assertThatThrownBy(() -> ChordSpelling.configured("alt+hh"))
                .isInstanceOf(InvalidChordException.class)
                .hasMessageContaining("'hh' is not a key");
    }

    @Test
    void shouldRefuseAChordWithoutAKey() {
        assertThatThrownBy(() -> ChordSpelling.configured("alt+")).isInstanceOf(InvalidChordException.class).hasMessageContaining("no key");
    }

    @Test
    void shouldReadClaudesMetaOptAndOptionAsAlt() {
        assertThat(ChordSpelling.boundByClaude("meta+h")).contains(altH());
        assertThat(ChordSpelling.boundByClaude("opt+h")).contains(altH());
        assertThat(ChordSpelling.boundByClaude("option+h")).contains(altH());
    }

    @Test
    void shouldReadClaudesControlAsCtrlAndIgnoreCase() {
        assertThat(ChordSpelling.boundByClaude("Control+ALT+H")).contains(ctrlAltH());
    }

    @Test
    void shouldTakeOnlyTheFirstKeystrokeOfASequence() {
        assertThat(ChordSpelling.boundByClaude("alt+h ctrl+k")).contains(altH());
    }

    @Test
    void shouldSayNothingOfABindingNoReservedChordCouldBe() {
        assertThat(ChordSpelling.boundByClaude("cmd+h")).isEqualTo(Optional.empty());
        assertThat(ChordSpelling.boundByClaude("escape")).isEqualTo(Optional.empty());
        assertThat(ChordSpelling.boundByClaude("shift+tab")).isEqualTo(Optional.empty());
        assertThat(ChordSpelling.boundByClaude("")).isEqualTo(Optional.empty());
    }
}

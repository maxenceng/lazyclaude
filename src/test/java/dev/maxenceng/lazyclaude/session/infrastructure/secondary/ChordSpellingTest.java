package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlAltH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.namedChord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertThat(ChordSpelling.configured("alt+ctrl+up")).isEqualTo(namedChord(NamedKey.UP, Modifier.CTRL, Modifier.ALT));
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
    void shouldAcceptShiftWithCtrlOnANamedKey() {
        assertThat(ChordSpelling.configured("ctrl+shift+up")).isEqualTo(namedChord(NamedKey.UP, Modifier.CTRL, Modifier.SHIFT));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "ctrl+shift+h | the terminal sends shift as the character it types",
        "alt+shift+h | the terminal sends shift as the character it types",
        "ctrl+alt+h | the terminal cannot report ctrl and alt together on a character",
        "ctrl+h | the terminal sends it as backspace",
        "ctrl+i | the terminal sends it as tab",
        "ctrl+j | the terminal sends it as enter",
        "ctrl+m | the terminal sends it as enter",
        "ctrl+[ | the terminal sends it as escape",
        "ctrl+1 | the terminal cannot report ctrl on '1'",
        "alt+enter | the terminal cannot report a modifier on enter",
        "ctrl+tab | the terminal cannot report a modifier on tab",
        "alt+escape | the terminal cannot report a modifier on escape",
        "alt+backspace | the terminal cannot report a modifier on backspace",
        "alt+[ | the terminal sends it as the start of an escape sequence"
    })
    void shouldRefuseAChordTheTerminalNeverDeliversSayingWhy(String spelled, String why) {
        assertThatThrownBy(() -> ChordSpelling.configured(spelled))
                .isInstanceOf(InvalidChordException.class)
                .hasMessage("'%s' never reaches lazyclaude: %s".formatted(spelled, why));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ctrl+g", "ctrl+y", "ctrl+\\", "ctrl+]", "ctrl+/", "alt+y", "alt+]", "alt+1", "ctrl+alt+up", "alt+f5"})
    void shouldAcceptAChordTheTerminalDelivers(String spelled) {
        assertThat(ChordSpelling.configured(spelled).text()).isEqualTo(spelled);
    }

    @Test
    void shouldReadAChordOfClaudesTheTerminalNeverDeliversAsItStands() {
        assertThat(ChordSpelling.boundByClaude("ctrl+alt+h")).contains(ctrlAltH());
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

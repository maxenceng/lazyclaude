package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.chord;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlAltH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.namedChord;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.pressed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChordTest {

    @Test
    void shouldRefuseShiftAlone() {
        assertThatThrownBy(() -> chord('h', Modifier.SHIFT))
                .isInstanceOf(InvalidChordException.class)
                .hasMessageContaining("shift alone");
    }

    @Test
    void shouldRefuseABareKey() {
        assertThatThrownBy(() -> chord('h'))
                .isInstanceOf(InvalidChordException.class)
                .hasMessageContaining("no modifier");
    }

    @Test
    void shouldAcceptShiftWithCtrl() {
        assertThat(chord('h', Modifier.CTRL, Modifier.SHIFT).text()).isEqualTo("ctrl+shift+h");
    }

    @Test
    void shouldSpellModifiersInCanonicalOrderWhateverOrderTheyWereGivenIn() {
        assertThat(chord('h', Modifier.ALT, Modifier.CTRL)).isEqualTo(ctrlAltH());
        assertThat(ctrlAltH().text()).isEqualTo("ctrl+alt+h");
    }

    @Test
    void shouldTreatTheKeyCaseInsensitively() {
        assertThat(chord('H', Modifier.ALT)).isEqualTo(altH());
    }

    @Test
    void shouldSpellANamedKeyWithoutUnderscores() {
        assertThat(namedChord(NamedKey.PAGE_UP, Modifier.ALT).text()).isEqualTo("alt+pageup");
    }

    @Test
    void shouldBePressedByTheSameKeyWithTheSameModifiersOnly() {
        assertThat(altH().isPressedBy(pressed('h', Modifier.ALT))).isTrue();
        assertThat(altH().isPressedBy(pressed('h', Modifier.CTRL, Modifier.ALT))).isFalse();
        assertThat(altH().isPressedBy(pressed('h', Modifier.ALT, Modifier.SHIFT))).isFalse();
        assertThat(altH().isPressedBy(pressed('j', Modifier.ALT))).isFalse();
        assertThat(altH().isPressedBy(pressed('H', Modifier.ALT)))
                .as("a terminal reports alt+shift+h as alt and H, which is claude's")
                .isFalse();
    }
}

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.published.ChordFixture.chord;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlAltH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.namedChord;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.rebinding;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

class ChordKeysTest {

    @Test
    void shouldShowTheModifiersOnceWhileTheFocusChordsShareThemAndAreOneCharacter() {
        assertThat(new ChordKeys(ReservedChords.defaults()).focus()).isEqualTo("alt+h/j/k/l");
    }

    @Test
    void shouldShowTheTabChordsInTheSameCompactForm() {
        assertThat(new ChordKeys(ReservedChords.defaults()).tab()).isEqualTo("alt+h/l");
    }

    @Test
    void shouldShowEveryFocusChordInFullWhenOneDiffersInItsModifiers() {
        ChordKeys keys = new ChordKeys(rebinding(ReservedAction.FOCUS_LEFT, ctrlAltH()));

        assertThat(keys.focus()).isEqualTo("ctrl+alt+h/alt+j/alt+k/alt+l");
    }

    @Test
    void shouldShowEveryFocusChordInFullWhenOneIsNotASingleCharacter() {
        ChordKeys keys = new ChordKeys(rebinding(ReservedAction.FOCUS_UP, namedChord(NamedKey.PAGE_UP, Modifier.ALT)));

        assertThat(keys.focus()).isEqualTo("alt+h/alt+j/alt+pageup/alt+l");
    }

    @Test
    void shouldShowTheTabChordsInFullWhenTheyDifferInTheirModifiers() {
        ChordKeys keys = new ChordKeys(rebinding(ReservedAction.FOCUS_RIGHT, chord('l', Modifier.CTRL, Modifier.ALT)));

        assertThat(keys.tab()).isEqualTo("alt+h/ctrl+alt+l");
    }

    @Test
    void shouldStayCompactWhenEveryFocusChordMovedToTheSameOtherModifiers() {
        ChordKeys keys = new ChordKeys(rebinding(Map.of(
                ReservedAction.FOCUS_LEFT, chord('a', Modifier.CTRL),
                ReservedAction.FOCUS_DOWN, chord('b', Modifier.CTRL),
                ReservedAction.FOCUS_UP, chord('c', Modifier.CTRL),
                ReservedAction.FOCUS_RIGHT, chord('d', Modifier.CTRL))));

        assertThat(keys.focus()).isEqualTo("ctrl+a/b/c/d");
    }

    @Test
    void shouldShowTheChordsOfHidingAndZoomingInFull() {
        ChordKeys keys = new ChordKeys(rebinding(ReservedAction.ZOOM, chord('x', Modifier.CTRL, Modifier.ALT)));

        assertThat(keys.hideMainView()).isEqualTo("alt+m");
        assertThat(keys.zoom()).isEqualTo("ctrl+alt+x");
    }
}

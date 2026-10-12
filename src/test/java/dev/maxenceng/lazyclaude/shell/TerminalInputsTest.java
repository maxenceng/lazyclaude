package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.leftPressAt;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.mouse;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.pressed;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.typed;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.wheelUpAt;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.PointerButton;
import dev.tamboui.layout.Rect;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

class TerminalInputsTest {

    private static final Rect PANE = new Rect(29, 10, 50, 9);

    @Test
    void shouldMapATypedCharacterToItsCodePoint() {
        assertThat(TerminalInputs.key(KeyEvent.ofChar('a'))).contains(typed('a', Modifiers.none()));
    }

    @Test
    void shouldMapACharacterOutsideTheBasicPlaneWhole() {
        assertThat(TerminalInputs.key(KeyEvent.ofChar(0x1F600))).contains(typed(0x1F600, Modifiers.none()));
    }

    @Test
    void shouldCarryEveryModifierHeld() {
        KeyEvent chord = KeyEvent.ofChar('c', new KeyModifiers(true, true, true));

        assertThat(TerminalInputs.key(chord))
                .contains(typed('c', Modifiers.of(Modifier.CTRL, Modifier.ALT, Modifier.SHIFT)));
    }

    @Test
    void shouldMapEveryNamedKey() {
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.ENTER))).contains(pressed(NamedKey.ENTER));
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.ESCAPE))).contains(pressed(NamedKey.ESCAPE));
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.BACKSPACE))).contains(pressed(NamedKey.BACKSPACE));
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.TAB))).contains(pressed(NamedKey.TAB));
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.PAGE_DOWN))).contains(pressed(NamedKey.PAGE_DOWN));
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.F12))).contains(pressed(NamedKey.F12));
    }

    @Test
    void shouldMapShiftTabToTabWithShift() {
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT)))
                .contains(pressed(NamedKey.TAB, Modifiers.of(Modifier.SHIFT)));
    }

    @Test
    void shouldMapNothingForAKeyTheInputLayerCouldNotName() {
        assertThat(TerminalInputs.key(KeyEvent.ofKey(KeyCode.UNKNOWN))).isEmpty();
    }

    @Test
    void shouldMapAMouseEventToTheCellRelativeToThePane() {
        assertThat(TerminalInputs.mouse(MouseEvent.press(MouseButton.LEFT, 33, 12), PANE)).contains(leftPressAt(4, 2));
    }

    @Test
    void shouldMapTheWheel() {
        assertThat(TerminalInputs.mouse(MouseEvent.scrollUp(29, 10), PANE)).contains(wheelUpAt(0, 0));
        assertThat(TerminalInputs.mouse(MouseEvent.scrollDown(29, 10), PANE))
                .contains(mouse(MouseAction.SCROLL_DOWN, PointerButton.NONE, 0, 0));
    }

    @Test
    void shouldMapEachButtonAndKindOfMouseEvent() {
        assertThat(TerminalInputs.mouse(MouseEvent.release(MouseButton.RIGHT, 30, 11), PANE))
                .contains(mouse(MouseAction.RELEASE, PointerButton.RIGHT, 1, 1));
        assertThat(TerminalInputs.mouse(MouseEvent.drag(MouseButton.MIDDLE, 30, 11), PANE))
                .contains(mouse(MouseAction.DRAG, PointerButton.MIDDLE, 1, 1));
        assertThat(TerminalInputs.mouse(MouseEvent.move(30, 11), PANE))
                .contains(mouse(MouseAction.MOVE, PointerButton.NONE, 1, 1));
    }

    @Test
    void shouldMapTheLastCellOfThePane() {
        assertThat(TerminalInputs.mouse(MouseEvent.press(MouseButton.LEFT, 78, 18), PANE)).contains(leftPressAt(49, 8));
    }

    @Test
    void shouldMapNothingForAMouseEventJustOutsideThePane() {
        assertThat(TerminalInputs.mouse(MouseEvent.press(MouseButton.LEFT, 28, 12), PANE)).isEmpty();
        assertThat(TerminalInputs.mouse(MouseEvent.press(MouseButton.LEFT, 79, 12), PANE)).isEmpty();
        assertThat(TerminalInputs.mouse(MouseEvent.press(MouseButton.LEFT, 40, 9), PANE)).isEmpty();
        assertThat(TerminalInputs.mouse(MouseEvent.press(MouseButton.LEFT, 40, 19), PANE)).isEmpty();
    }
}

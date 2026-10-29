package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.pressed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.typed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SentBytesFixture.sent;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;

class KeyBytesTest {

    @Test
    void shouldSendATypedCharacterAsUtf8() {
        assertThat(KeyBytes.of(typed('é', Modifiers.none()), false)).isEqualTo("é".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldSendACharacterOutsideTheBasicPlaneAsFourUtf8Bytes() {
        assertThat(KeyBytes.of(typed(0x1F600, Modifiers.none()), false)).hasSize(4);
    }

    @Test
    void shouldSendCtrlAndALetterAsItsControlByte() {
        assertThat(KeyBytes.of(typed('c', Modifiers.of(Modifier.CTRL)), false)).containsExactly(3);
        assertThat(KeyBytes.of(typed('R', Modifiers.of(Modifier.CTRL)), false)).containsExactly(18);
    }

    @Test
    void shouldSendCtrlAndSpaceAsNul() {
        assertThat(KeyBytes.of(typed(' ', Modifiers.of(Modifier.CTRL)), false)).containsExactly(0);
    }

    @Test
    void shouldSendAltAndACharacterAsEscapeThenTheCharacter() {
        assertThat(sent(KeyBytes.of(typed('b', Modifiers.of(Modifier.ALT)), false))).isEqualTo("\u001bb");
    }

    @Test
    void shouldSendEnterAsCarriageReturn() {
        assertThat(KeyBytes.of(pressed(NamedKey.ENTER), false)).containsExactly(13);
    }

    @Test
    void shouldSendEscapeAsTheEscapeByte() {
        assertThat(KeyBytes.of(pressed(NamedKey.ESCAPE), false)).containsExactly(27);
    }

    @Test
    void shouldSendTabAsHorizontalTab() {
        assertThat(KeyBytes.of(pressed(NamedKey.TAB), false)).containsExactly(9);
    }

    @Test
    void shouldSendShiftTabAsBackTab() {
        assertThat(sent(KeyBytes.of(pressed(NamedKey.TAB, Modifiers.of(Modifier.SHIFT)), false))).isEqualTo("\u001b[Z");
    }

    @Test
    void shouldSendBackspaceAsDelete() {
        assertThat(KeyBytes.of(pressed(NamedKey.BACKSPACE), false)).containsExactly(127);
    }

    @Test
    void shouldSendAnArrowAsACsiSequence() {
        assertThat(sent(KeyBytes.of(pressed(NamedKey.UP), false))).isEqualTo("\u001b[A");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.LEFT), false))).isEqualTo("\u001b[D");
    }

    @Test
    void shouldSendAnArrowAsAnSs3SequenceWhileTheChildHoldsApplicationCursorKeys() {
        assertThat(sent(KeyBytes.of(pressed(NamedKey.UP), true))).isEqualTo("\u001bOA");
    }

    @Test
    void shouldSendAnArrowWithAModifierParameter() {
        assertThat(sent(KeyBytes.of(pressed(NamedKey.RIGHT, Modifiers.of(Modifier.CTRL)), false)))
                .as("ctrl is parameter 5: one plus four")
                .isEqualTo("\u001b[1;5C");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.LEFT, Modifiers.of(Modifier.ALT, Modifier.SHIFT)), false)))
                .isEqualTo("\u001b[1;4D");
    }

    @Test
    void shouldSendTheEditingKeysAsTildeSequences() {
        assertThat(sent(KeyBytes.of(pressed(NamedKey.DELETE), false))).isEqualTo("\u001b[3~");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.PAGE_UP), false))).isEqualTo("\u001b[5~");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.PAGE_DOWN), false))).isEqualTo("\u001b[6~");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.INSERT), false))).isEqualTo("\u001b[2~");
    }

    @Test
    void shouldSendHomeAndEnd() {
        assertThat(sent(KeyBytes.of(pressed(NamedKey.HOME), false))).isEqualTo("\u001b[H");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.END), true))).isEqualTo("\u001bOF");
    }

    @Test
    void shouldSendTheFunctionKeys() {
        assertThat(sent(KeyBytes.of(pressed(NamedKey.F1), false))).isEqualTo("\u001bOP");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.F5), false))).isEqualTo("\u001b[15~");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.F12), false))).isEqualTo("\u001b[24~");
        assertThat(sent(KeyBytes.of(pressed(NamedKey.F2, Modifiers.of(Modifier.SHIFT)), false))).isEqualTo("\u001b[1;2Q");
    }
}

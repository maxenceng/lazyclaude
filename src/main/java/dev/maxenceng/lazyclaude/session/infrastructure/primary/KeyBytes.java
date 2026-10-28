package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.charset.StandardCharsets;

import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.PressedKey;
import dev.maxenceng.lazyclaude.session.published.TypedCharacter;

/**
 * A key press as the bytes an xterm sends: a character as UTF-8, with Ctrl as its control byte and Alt as an escape before
 * it; the other keys as the sequences of xterm's key tables, arrows and Home/End as SS3 while the child holds application
 * cursor keys, and a modifier as the parameter {@code 1 + shift + 2 alt + 4 ctrl}.
 */
final class KeyBytes {

    private static final int INSERT_CODE = 2;
    private static final int DELETE_CODE = 3;
    private static final int PAGE_UP_CODE = 5;
    private static final int PAGE_DOWN_CODE = 6;
    private static final int F5_CODE = 15;
    private static final int F6_CODE = 17;
    private static final int F7_CODE = 18;
    private static final int F8_CODE = 19;
    private static final int F9_CODE = 20;
    private static final int F10_CODE = 21;
    private static final int F11_CODE = 23;
    private static final int F12_CODE = 24;
    private static final String MODIFIED_KEY = "%s1;%d%c";
    private static final String TILDE_KEY = "%s%d~";
    private static final String MODIFIED_TILDE_KEY = "%s%d;%d~";
    private static final String ESCAPE = "\u001b";
    private static final String CSI = ESCAPE + "[";
    private static final String SS3 = ESCAPE + "O";
    private static final int CONTROL_MASK = 0x1f;
    private static final int DELETE = 0x7f;
    private static final int NUL = 0;
    private static final int HORIZONTAL_TAB = 9;
    private static final int CARRIAGE_RETURN = 13;
    private static final int BACKSPACE = 8;
    private static final int SHIFT_PARAMETER = 1;
    private static final int ALT_PARAMETER = 2;
    private static final int CTRL_PARAMETER = 4;
    private static final int PLAIN_PARAMETER = 1;
    private static final char FIRST_CONTROLLABLE = '@';
    private static final char LAST_CONTROLLABLE = '_';
    private static final char SPACE = ' ';
    private static final char QUESTION_MARK = '?';
    private static final String BACK_TAB = CSI + "Z";

    private KeyBytes() {}

    static byte[] of(TerminalKey key, boolean applicationCursor) {
        return switch (key.input()) {
            case TypedCharacter typed -> character(typed.codePoint(), key.modifiers());
            case PressedKey pressed -> named(pressed.key(), key.modifiers(), applicationCursor);
        };
    }

    private static byte[] character(int codePoint, Modifiers modifiers) {
        byte[] body = modifiers.has(Modifier.CTRL) ? controlled(codePoint) : utf8(codePoint);
        return modifiers.has(Modifier.ALT) ? prefixed(body) : body;
    }

    private static byte[] controlled(int codePoint) {
        int upper = Character.toUpperCase(codePoint);
        if (codePoint == SPACE) {
            return new byte[] {NUL};
        }
        if (codePoint == QUESTION_MARK) {
            return new byte[] {DELETE};
        }
        if (upper >= FIRST_CONTROLLABLE && upper <= LAST_CONTROLLABLE) {
            return new byte[] {(byte) (upper & CONTROL_MASK)};
        }
        return utf8(codePoint);
    }

    private static byte[] utf8(int codePoint) {
        return Character.toString(codePoint).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] prefixed(byte[] body) {
        byte[] escape = ESCAPE.getBytes(StandardCharsets.US_ASCII);
        byte[] prefixed = new byte[escape.length + body.length];
        System.arraycopy(escape, 0, prefixed, 0, escape.length);
        System.arraycopy(body, 0, prefixed, escape.length, body.length);
        return prefixed;
    }

    private static byte[] named(NamedKey key, Modifiers modifiers, boolean applicationCursor) {
        return switch (key) {
            case ENTER -> withAlt(new byte[] {CARRIAGE_RETURN}, modifiers);
            case ESCAPE -> withAlt(ESCAPE.getBytes(StandardCharsets.US_ASCII), modifiers);
            case TAB -> modifiers.has(Modifier.SHIFT) ? ascii(BACK_TAB) : withAlt(new byte[] {HORIZONTAL_TAB}, modifiers);
            case BACKSPACE ->
                withAlt(new byte[] {(byte) (modifiers.has(Modifier.CTRL) ? BACKSPACE : DELETE)}, modifiers);
            case UP -> cursor('A', modifiers, applicationCursor);
            case DOWN -> cursor('B', modifiers, applicationCursor);
            case RIGHT -> cursor('C', modifiers, applicationCursor);
            case LEFT -> cursor('D', modifiers, applicationCursor);
            case HOME -> cursor('H', modifiers, applicationCursor);
            case END -> cursor('F', modifiers, applicationCursor);
            case INSERT -> tilde(INSERT_CODE, modifiers);
            case DELETE -> tilde(DELETE_CODE, modifiers);
            case PAGE_UP -> tilde(PAGE_UP_CODE, modifiers);
            case PAGE_DOWN -> tilde(PAGE_DOWN_CODE, modifiers);
            case F1 -> function('P', modifiers);
            case F2 -> function('Q', modifiers);
            case F3 -> function('R', modifiers);
            case F4 -> function('S', modifiers);
            case F5 -> tilde(F5_CODE, modifiers);
            case F6 -> tilde(F6_CODE, modifiers);
            case F7 -> tilde(F7_CODE, modifiers);
            case F8 -> tilde(F8_CODE, modifiers);
            case F9 -> tilde(F9_CODE, modifiers);
            case F10 -> tilde(F10_CODE, modifiers);
            case F11 -> tilde(F11_CODE, modifiers);
            case F12 -> tilde(F12_CODE, modifiers);
        };
    }

    private static byte[] withAlt(byte[] body, Modifiers modifiers) {
        return modifiers.has(Modifier.ALT) ? prefixed(body) : body;
    }

    private static byte[] cursor(char final_, Modifiers modifiers, boolean applicationCursor) {
        int parameter = parameter(modifiers);
        if (parameter != PLAIN_PARAMETER) {
            return ascii(MODIFIED_KEY.formatted(CSI, parameter, final_));
        }
        return ascii((applicationCursor ? SS3 : CSI) + final_);
    }

    private static byte[] function(char final_, Modifiers modifiers) {
        int parameter = parameter(modifiers);
        return parameter == PLAIN_PARAMETER ? ascii(SS3 + final_) : ascii(MODIFIED_KEY.formatted(CSI, parameter, final_));
    }

    private static byte[] tilde(int number, Modifiers modifiers) {
        int parameter = parameter(modifiers);
        return parameter == PLAIN_PARAMETER ? ascii(TILDE_KEY.formatted(CSI, number)) : ascii(MODIFIED_TILDE_KEY.formatted(CSI, number, parameter));
    }

    private static int parameter(Modifiers modifiers) {
        return PLAIN_PARAMETER
                + (modifiers.has(Modifier.SHIFT) ? SHIFT_PARAMETER : 0)
                + (modifiers.has(Modifier.ALT) ? ALT_PARAMETER : 0)
                + (modifiers.has(Modifier.CTRL) ? CTRL_PARAMETER : 0);
    }

    private static byte[] ascii(String sequence) {
        return sequence.getBytes(StandardCharsets.US_ASCII);
    }
}

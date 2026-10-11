package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;

import com.jediterm.terminal.emulator.mouse.MouseMode;

import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.PointerButton;
import dev.maxenceng.lazyclaude.session.published.TerminalMouse;

/**
 * A mouse event as the SGR bytes a terminal sends ({@code ESC [ < button ; column ; row M}), only as many kinds of event as
 * the child asked for: nothing while it reports no mouse, clicks and wheel for plain tracking, drags for button-motion and
 * bare motion for all-motion.
 */
final class MouseBytes {

    private static final int LEFT = 0;
    private static final int MIDDLE = 1;
    private static final int RIGHT = 2;
    private static final int NO_BUTTON = 3;
    private static final int SHIFT = 4;
    private static final int ALT = 8;
    private static final int CTRL = 16;
    private static final int MOTION = 32;
    private static final int WHEEL_UP = 64;
    private static final int WHEEL_DOWN = 65;
    private static final int WHEEL_LEFT = 66;
    private static final int WHEEL_RIGHT = 67;
    private static final String SEQUENCE = "\u001b[<%d;%d;%d%s";
    private static final String PRESSED = "M";
    private static final String RELEASED = "m";

    private MouseBytes() {}

    static byte[] of(TerminalMouse mouse, MouseMode mode) {
        if (!reports(mouse.action(), mode)) {
            return new byte[0];
        }
        String sequence = SEQUENCE.formatted(
                code(mouse),
                mouse.column().value() + 1,
                mouse.row().value() + 1,
                mouse.action() == MouseAction.RELEASE ? RELEASED : PRESSED);
        return sequence.getBytes(StandardCharsets.US_ASCII);
    }

    private static boolean reports(MouseAction action, MouseMode mode) {
        return switch (mode) {
            case MOUSE_REPORTING_NONE, MOUSE_REPORTING_FOCUS -> false;
            case MOUSE_REPORTING_NORMAL, MOUSE_REPORTING_HILITE ->
                action != MouseAction.DRAG && action != MouseAction.MOVE;
            case MOUSE_REPORTING_BUTTON_MOTION -> action != MouseAction.MOVE;
            case MOUSE_REPORTING_ALL_MOTION -> true;
        };
    }

    private static int code(TerminalMouse mouse) {
        return base(mouse) + modifierBits(mouse);
    }

    private static int base(TerminalMouse mouse) {
        return switch (mouse.action()) {
            case SCROLL_UP -> WHEEL_UP;
            case SCROLL_DOWN -> WHEEL_DOWN;
            case SCROLL_LEFT -> WHEEL_LEFT;
            case SCROLL_RIGHT -> WHEEL_RIGHT;
            case DRAG -> MOTION + button(mouse.button());
            case MOVE -> MOTION + NO_BUTTON;
            case PRESS, RELEASE -> button(mouse.button());
        };
    }

    private static int button(PointerButton button) {
        return switch (button) {
            case LEFT -> LEFT;
            case MIDDLE -> MIDDLE;
            case RIGHT -> RIGHT;
            case NONE -> NO_BUTTON;
        };
    }

    private static int modifierBits(TerminalMouse mouse) {
        return (mouse.modifiers().has(Modifier.SHIFT) ? SHIFT : 0)
                + (mouse.modifiers().has(Modifier.ALT) ? ALT : 0)
                + (mouse.modifiers().has(Modifier.CTRL) ? CTRL : 0);
    }
}

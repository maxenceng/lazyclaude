package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

final class ShellFixture {

    private ShellFixture() {}

    static Shell shellOn(BoardView board) {
        return new Shell(board);
    }

    static Shell shellOn(TicketView... tickets) {
        return shellOn(boardViewOf(tickets));
    }

    static KeyEvent key(char c) {
        return KeyEvent.ofChar(c);
    }

    static KeyEvent alt(char c) {
        return KeyEvent.ofChar(c, KeyModifiers.ALT);
    }

    static KeyEvent ctrl(char c) {
        return KeyEvent.ofChar(c, KeyModifiers.CTRL);
    }

    static Shell shellFocusedOnMainViewOf(TicketView... tickets) {
        Shell shell = pressed(shellOn(tickets), key('j'), 1);
        Screen.of(shell);
        shell.press(key('l'));
        return shell;
    }

    static KeyEvent enter() {
        return KeyEvent.ofKey(KeyCode.ENTER);
    }

    static Shell pressed(Shell shell, KeyEvent key, int times) {
        for (int pressed = 0; pressed < times; pressed++) {
            shell.press(key);
        }
        return shell;
    }
}

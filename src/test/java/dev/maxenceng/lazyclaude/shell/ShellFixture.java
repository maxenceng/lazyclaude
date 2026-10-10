package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.emptySessionsView;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

final class ShellFixture {

    private ShellFixture() {}

    static Shell shellOn(BoardView board) {
        return new Shell(board, emptyDocsView(), emptySessionsView());
    }

    static Shell shellOnDocs(DocsView docs) {
        return pressed(new Shell(boardViewOf(ticketView()), docs, emptySessionsView()), alt('j'), 2);
    }

    static Shell shellOnSessions(SessionsView sessions) {
        return pressed(new Shell(boardViewOf(ticketView()), emptyDocsView(), sessions), alt('j'), 1);
    }

    static Shell shellFocusedOnMainViewOfSessions(SessionsView sessions) {
        Shell shell = shellOnSessions(sessions);
        Screen.of(shell);
        shell.press(key('l'));
        return shell;
    }

    static Shell shellFocusedOnMainViewOfDocs(DocsView docs) {
        Shell shell = shellOnDocs(docs);
        Screen.of(shell);
        shell.press(key('l'));
        return shell;
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

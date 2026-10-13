package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedNamedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.strongLink;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

final class ShellFixture {

    private ShellFixture() {}

    static Shell newShell(BoardView board, DocsView docs, SessionsView sessions) {
        return new Shell(
                board,
                docs,
                sessions,
                checkingPullRequestsView(),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                blankScreens());
    }

    static Shell shellOnPullRequests(PullRequestsView pullRequests, TicketView... tickets) {
        Shell shell = new Shell(
                boardViewOf(tickets),
                emptyDocsView(),
                emptySessionsView(),
                pullRequests,
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                blankScreens());
        shell.press(key('j'));
        return shell;
    }

    static Shell shellOnPullRequestsAndSessions(PullRequestsView pullRequests, SessionsView sessions, TicketView... tickets) {
        Shell shell = new Shell(
                boardViewOf(tickets),
                emptyDocsView(),
                sessions,
                pullRequests,
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                blankScreens());
        shell.press(key('j'));
        return shell;
    }

    static SessionsView yourTurnOnTheFirstTicket() {
        return sessionsViewOf(List.of(linkedNamedSessionView(1, "calm-one", new YourTurn(), strongLink("PROJ-1", "start"))));
    }

    static Object noHostStarted(InvocationOnMock invocation) {
        return invocation.getMethod().getReturnType() == HostStart.class ? new NotStartedHost() : null;
    }

    static SessionQuery blankScreens() {
        return mock(SessionQuery.class, invocation -> TerminalScreen.blank());
    }

    static Shell shellHosting(SessionsView sessions, HostedSessionCommand hosting, SessionQuery screens) {
        return new Shell(boardViewOf(ticketView()), emptyDocsView(), sessions, checkingPullRequestsView(), hosting, screens);
    }

    static Shell shellInDock(SessionsView sessions, HostedSessionCommand hosting, SessionQuery screens) {
        Shell shell = shellHosting(sessions, hosting, screens);
        Screen.of(shell);
        shell.press(alt('l'));
        shell.press(alt('j'));
        return shell;
    }

    static Shell oneTabInDock(HostedSessionCommand hosting, SessionQuery screens) {
        return shellInDock(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
    }

    static Shell shellOn(BoardView board) {
        return newShell(board, emptyDocsView(), emptySessionsView());
    }

    static Shell shellWarning(WarningView... warnings) {
        Shell shell = shellOn(boardViewOf(ticketView()));
        for (WarningView warning : warnings) {
            shell.warningsChanged(warning);
        }
        return shell;
    }

    static Shell shellOnDocs(DocsView docs) {
        return pressed(newShell(boardViewOf(ticketView()), docs, emptySessionsView()), alt('j'), 2);
    }

    static Shell shellOnSessions(SessionsView sessions) {
        return pressed(newShell(boardViewOf(ticketView()), emptyDocsView(), sessions), alt('j'), 1);
    }

    static Shell shellOnTicketSessions(SessionsView sessions) {
        Shell shell = newShell(boardViewOf(ticketView(), secondTicketView()), emptyDocsView(), sessions);
        shell.press(key('j'));
        return shell;
    }

    static Shell shellOnTicketSessionsPanel(SessionsView sessions) {
        return pressed(shellOnTicketSessions(sessions), alt('j'), 1);
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

    static NewHostedPayload startedPayload(HostedSessionCommand hosting) {
        ArgumentCaptor<NewHostedPayload> payload = ArgumentCaptor.forClass(NewHostedPayload.class);
        verify(hosting).startUnlinked(payload.capture());
        return payload.getValue();
    }

    static KeyEvent namedKey(KeyCode code) {
        return KeyEvent.ofKey(code);
    }

    static KeyEvent shiftTab() {
        return KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT);
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

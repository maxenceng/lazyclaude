package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.projectRoot;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.JumpPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoTranscriptView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.OutsideSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RemoteControlConfigView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoicePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoiceView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
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
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                blankScreens(),
                mock(BoardQuery.class), mock(DocsQuery.class), mock(ReviewQuery.class), Runnable::run);
    }

    static Shell shellReading(BoardQuery boards, TicketView... tickets) {
        return new Shell(
                boardViewOf(tickets),
                emptyDocsView(),
                emptySessionsView(),
                checkingPullRequestsView(),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                blankScreens(),
                boards, mock(DocsQuery.class), mock(ReviewQuery.class), Runnable::run);
    }

    static Shell shellRefreshing(
            BoardQuery boards, DocsQuery docs, ReviewQuery reviews, SessionQuery screens, Executor background, BoardView board) {
        return new Shell(
                board,
                emptyDocsView(),
                emptySessionsView(),
                checkingPullRequestsView(),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                screens,
                boards,
                docs,
                reviews,
                background);
    }

    static Shell shellReadingBoardWith(BoardQuery boards, SessionQuery screens, TicketView... tickets) {
        return shellRefreshing(boards, mock(DocsQuery.class), mock(ReviewQuery.class), screens, tickets);
    }

    static Shell shellRefreshing(
            BoardQuery boards, DocsQuery docs, ReviewQuery reviews, SessionQuery screens, TicketView... tickets) {
        return shellRefreshing(boards, docs, reviews, screens, Runnable::run, boardViewOf(tickets));
    }

    static Shell shellReadingBoardOnDocs(BoardQuery boards, DocsView docs, TicketView... tickets) {
        return new Shell(
                boardViewOf(tickets),
                docs,
                emptySessionsView(),
                checkingPullRequestsView(),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                blankScreens(),
                boards,
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                Runnable::run);
    }

    static BoardPayload readingBoardWithMerged(String... branches) {
        return new BoardPayload(projectRoot().value(), Set.of(branches));
    }

    static Shell shellOnPullRequests(PullRequestsView pullRequests, TicketView... tickets) {
        Shell shell = new Shell(
                boardViewOf(tickets),
                emptyDocsView(),
                emptySessionsView(),
                pullRequests,
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                blankScreens(),
                mock(BoardQuery.class), mock(DocsQuery.class), mock(ReviewQuery.class), Runnable::run);
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
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                blankScreens(),
                mock(BoardQuery.class), mock(DocsQuery.class), mock(ReviewQuery.class), Runnable::run);
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
        return mock(SessionQuery.class, ShellFixture::blankScreenOrFirstVerb);
    }

    static SessionQuery screensReadingTranscripts(TranscriptView first, TranscriptView... later) {
        SessionQuery screens = blankScreens();
        when(screens.transcriptOf(any())).thenReturn(first, later);
        return screens;
    }

    static OutsideSessionCommand outsideOffering(RemoteControlConfigView config) {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        when(outside.jump(any())).thenReturn(Optional.of(config));
        return outside;
    }

    static Shell shellWithSessionsFocused(
            SessionsView sessions, SessionQuery screens, OutsideSessionCommand outside, Executor background, int panels) {
        return pressed(shellWith(sessions, screens, outside, background), alt('j'), panels);
    }

    static Shell shellOnSessionsReading(SessionsView sessions, SessionQuery screens) {
        return shellWithSessionsFocused(sessions, screens, mock(OutsideSessionCommand.class), Runnable::run, 1);
    }

    static Shell shellOnSessionsOutside(SessionsView sessions, OutsideSessionCommand outside) {
        return shellWithSessionsFocused(sessions, blankScreens(), outside, Runnable::run, 1);
    }

    static Shell shellOnElsewhereOutside(SessionsView sessions, OutsideSessionCommand outside) {
        return shellWithSessionsFocused(sessions, blankScreens(), outside, Runnable::run, 3);
    }

    private static Shell shellWith(SessionsView sessions, SessionQuery screens, OutsideSessionCommand outside, Executor background) {
        return new Shell(
                boardViewOf(ticketView()),
                emptyDocsView(),
                sessions,
                checkingPullRequestsView(),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                mock(WatchedSessionCommand.class),
                outside,
                screens,
                mock(BoardQuery.class),
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                background);
    }

    static List<TranscriptPayload> transcriptsAsked(SessionQuery screens, int times) {
        ArgumentCaptor<TranscriptPayload> asked = ArgumentCaptor.forClass(TranscriptPayload.class);
        verify(screens, times(times)).transcriptOf(asked.capture());
        return asked.getAllValues();
    }

    static JumpPayload jumpedWith(OutsideSessionCommand outside) {
        ArgumentCaptor<JumpPayload> payload = ArgumentCaptor.forClass(JumpPayload.class);
        verify(outside).jump(payload.capture());
        return payload.getValue();
    }

    static SessionQuery screensChoosing(VerbChoiceView choice) {
        return mock(SessionQuery.class, invocation -> invocation.getMethod().getReturnType() == VerbChoiceView.class
                ? choice
                : blankScreenOrNothing(invocation));
    }

    private static Object blankScreenOrNothing(InvocationOnMock invocation) {
        return invocation.getMethod().getReturnType() == TerminalScreen.class ? TerminalScreen.blank() : null;
    }

    private static Object blankScreenOrFirstVerb(InvocationOnMock invocation) {
        if (invocation.getMethod().getReturnType() == TranscriptView.class) {
            return new NoTranscriptView();
        }
        if (invocation.getMethod().getReturnType() != VerbChoiceView.class) {
            return blankScreenOrNothing(invocation);
        }
        VerbChoicePayload payload = invocation.getArgument(0);
        Verb preselected = payload.merged() && payload.verbs().contains(Verb.DONE) ? Verb.DONE : payload.verbs().getFirst();
        return VerbChoiceView.builder().verbs(payload.verbs()).preselected(preselected).build();
    }

    static Shell shellOnTicket(TicketView ticket, SessionsView sessions, HostedSessionCommand hosting, SessionQuery screens) {
        Shell shell = new Shell(
                boardViewOf(ticket),
                emptyDocsView(),
                sessions,
                checkingPullRequestsView(),
                hosting,
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                screens,
                mock(BoardQuery.class), mock(DocsQuery.class), mock(ReviewQuery.class), Runnable::run);
        pressed(shell, key('j'), ticket.status().ordinal());
        if (ticket.status().finished()) {
            shell.press(enter());
        }
        shell.press(key('j'));
        return shell;
    }

    static Shell shellOnTicket(TicketView ticket, HostedSessionCommand hosting) {
        return shellOnTicket(ticket, emptySessionsView(), hosting, blankScreens());
    }

    static KeyEvent escape() {
        return KeyEvent.ofKey(KeyCode.ESCAPE);
    }

    static TicketHostedPayload startedTicketPayload(HostedSessionCommand hosting) {
        ArgumentCaptor<TicketHostedPayload> payload = ArgumentCaptor.forClass(TicketHostedPayload.class);
        verify(hosting).startTicket(payload.capture());
        return payload.getValue();
    }

    static Shell shellHosting(SessionsView sessions, HostedSessionCommand hosting, SessionQuery screens) {
        return shellHosting(sessions, hosting, mock(WatchedSessionCommand.class), screens);
    }

    static Shell shellHosting(
            SessionsView sessions, HostedSessionCommand hosting, WatchedSessionCommand watching, SessionQuery screens) {
        return shellHosting(sessions, hosting, watching, screens, Runnable::run);
    }

    static Shell shellInDockRunningOn(SessionsView sessions, WatchedSessionCommand watching, Executor background) {
        Shell shell = shellHosting(
                sessions, mock(HostedSessionCommand.class, ShellFixture::noHostStarted), watching, blankScreens(), background);
        Screen.of(shell);
        shell.press(alt('l'));
        shell.press(alt('j'));
        return shell;
    }

    private static Shell shellHosting(
            SessionsView sessions,
            HostedSessionCommand hosting,
            WatchedSessionCommand watching,
            SessionQuery screens,
            Executor background) {
        return new Shell(
                boardViewOf(ticketView()),
                emptyDocsView(),
                sessions,
                checkingPullRequestsView(),
                hosting,
                watching,
                mock(OutsideSessionCommand.class),
                screens,
                mock(BoardQuery.class),
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                background);
    }

    static Shell shellInDock(SessionsView sessions, HostedSessionCommand hosting, SessionQuery screens) {
        return shellInDock(sessions, hosting, mock(WatchedSessionCommand.class), screens);
    }

    static Shell shellInDock(
            SessionsView sessions, HostedSessionCommand hosting, WatchedSessionCommand watching, SessionQuery screens) {
        Shell shell = shellHosting(sessions, hosting, watching, screens);
        Screen.of(shell);
        shell.press(alt('l'));
        shell.press(alt('j'));
        return shell;
    }

    static Shell oneTabInDockWithReviews(HostedSessionCommand hosting, SessionQuery screens, ReviewQuery reviews) {
        Shell shell = new Shell(
                boardViewOf(ticketView()),
                emptyDocsView(),
                sessionsViewWithTabs(hostedTab(1, "one", new Busy())),
                checkingPullRequestsView(),
                hosting,
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                screens,
                mock(BoardQuery.class),
                mock(DocsQuery.class),
                reviews,
                Runnable::run);
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

    static Shell shellOnElsewhere(SessionsView sessions) {
        return pressed(newShell(boardViewOf(ticketView()), emptyDocsView(), sessions), alt('j'), 3);
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

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Executor;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureCommand;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.tamboui.tui.event.KeyEvent;

final class ShellCaptureFixture {

    private ShellCaptureFixture() {}

    static Shell shellCapturing(TicketCaptureCommand capturing, BoardQuery boards, Executor background, TicketView... tickets) {
        return new Shell(
                boardViewOf(tickets),
                emptyDocsView(),
                emptySessionsView(),
                checkingPullRequestsView(),
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                mock(HostedEmulatorCommand.class),
                ShellFixture.blankEmulators(),
                mock(WatchedSessionCommand.class),
                mock(TicketWorktreeCommand.class),
                ShellFixture.blankScreens(),
                boards,
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                mock(PendingReviewCommand.class),
                capturing,
                ReservedChords.defaults(),
                true,
                background);
    }

    static Shell shellOpenForATitle(TicketCaptureCommand capturing, BoardQuery boards, Executor background, TicketView... tickets) {
        Shell shell = shellCapturing(capturing, boards, background, tickets);
        shell.press(ShellFixture.key('c'));
        return shell;
    }

    static Shell typed(Shell shell, String text) {
        text.chars().forEach(character -> shell.press(KeyEvent.ofChar((char) character)));
        return shell;
    }
}

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionAgeQueryFixture.sessionAgeQuery;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executor;

import org.mockito.ArgumentCaptor;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewTicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.tamboui.tui.event.KeyEvent;

final class ShellCaptureFixture {

    private ShellCaptureFixture() {}

    static Shell shellCapturing(HostedSessionCommand hosting, BoardQuery boards, Executor background, TicketView... tickets) {
        return new Shell(
                boardViewOf(tickets),
                emptyDocsView(),
                emptySessionsView(),
                checkingPullRequestsView(),
                hosting,
                mock(HostedEmulatorCommand.class),
                ShellFixture.blankEmulators(),
                mock(WatchedSessionCommand.class),
                mock(TicketWorktreeCommand.class),
                ShellFixture.blankScreens(), sessionAgeQuery(),
                boards,
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                mock(PendingReviewCommand.class),
                ReservedChords.defaults(),
                true,
                background);
    }

    static Shell shellOpenForADescription(HostedSessionCommand hosting, BoardQuery boards, Executor background, TicketView... tickets) {
        Shell shell = shellCapturing(hosting, boards, background, tickets);
        shell.press(ShellFixture.key('c'));
        return shell;
    }

    static NewTicketHostedPayload newTicketPayload(HostedSessionCommand hosting) {
        ArgumentCaptor<NewTicketHostedPayload> payload = ArgumentCaptor.forClass(NewTicketHostedPayload.class);
        verify(hosting).startNewTicket(payload.capture());
        return payload.getValue();
    }

    static Shell typed(Shell shell, String text) {
        text.chars().forEach(character -> shell.press(KeyEvent.ofChar((char) character)));
        return shell;
    }
}

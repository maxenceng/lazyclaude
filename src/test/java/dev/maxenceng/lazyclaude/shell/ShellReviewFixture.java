package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.commentOnView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.loadingPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.onDiffAnchorView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.openPendingViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingCommentView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.replyViewOn;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.resolvedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.pendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.secondPendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.OpenPendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewWriteView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReadThreadsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.OutsideSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;

final class ShellReviewFixture {

    private ShellReviewFixture() {}

    static ReviewQuery reviewsAnswering(PullRequestDiffView diff, ThreadsView threads) {
        return reviewsAnswering(diff, threads, noPendingReviewView());
    }

    static ReviewQuery reviewsAnswering(PullRequestDiffView diff, ThreadsView threads, PendingReviewView pending) {
        ReviewQuery reviews = mock(ReviewQuery.class);
        when(reviews.loadingDiff()).thenReturn(loadingDiffView());
        when(reviews.loadingThreads()).thenReturn(loadingThreadsView());
        when(reviews.loadingPending()).thenReturn(loadingPendingView());
        when(reviews.diffOf(any())).thenReturn(diff);
        when(reviews.threadsOf(any())).thenReturn(threads);
        when(reviews.pendingOf(any())).thenReturn(pending);
        ReviewQuery joining = ReviewPaneFixture.joiningReviews();
        when(reviews.threadsInDiff(any(), any()))
                .thenAnswer(call -> joining.threadsInDiff(call.getArgument(0), call.getArgument(1)));
        when(reviews.pendingInDiff(any(), any()))
                .thenAnswer(call -> joining.pendingInDiff(call.getArgument(0), call.getArgument(1)));
        when(reviews.anchorOf(any(), any(), any()))
                .thenAnswer(call -> joining.anchorOf(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        return reviews;
    }

    static Shell shellOnATicketWith(PullRequestsView pullRequests, ReviewQuery reviews, Executor background) {
        return shellOnATicketWith(pullRequests, reviews, mock(PendingReviewCommand.class), background);
    }

    static Shell shellOnATicketWith(
            PullRequestsView pullRequests, ReviewQuery reviews, PendingReviewCommand writing, Executor background) {
        return shellOnTheTicket(slugTicketView(), pullRequests, reviews, writing, mock(HostedSessionCommand.class, ShellFixture::noHostStarted), background);
    }

    static Shell shellOnTheTicket(
            TicketView ticket,
            PullRequestsView pullRequests,
            ReviewQuery reviews,
            PendingReviewCommand writing,
            HostedSessionCommand hosting,
            Executor background) {
        Shell shell = new Shell(
                boardViewOf(ticket),
                emptyDocsView(),
                emptySessionsView(),
                pullRequests,
                hosting,
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                mock(TicketWorktreeCommand.class),
                blankScreens(),
                mock(BoardQuery.class),
                mock(DocsQuery.class),
                reviews,
                writing,
                background);
        return ShellFixture.pressed(shell, key('j'), ticket.status().ordinal() + 1);
    }

    static Shell shellSubmitting(TicketView ticket, PendingReviewCommand writing, HostedSessionCommand hosting) {
        Shell shell = shellOnTheTicket(
                ticket,
                openPullRequestsViewOf(pullRequestView()),
                reviewsAnswering(twoFileDiffView(), readThreadsViewOf(), openPendingViewOf(commentOnView(diffPath(), rightOn(3)))),
                writing,
                hosting,
                Runnable::run);
        shell.press(key('p'));
        return shell;
    }

    static Shell shellOnATicketWithAnOpenPullRequest(ReviewQuery reviews, Executor background) {
        return shellOnATicketWith(openPullRequestsViewOf(pullRequestView()), reviews, background);
    }

    static Shell shellReviewing(PullRequestDiffView diff, ThreadsView threads) {
        return shellReviewing(diff, threads, noPendingReviewView());
    }

    static Shell shellReviewing(PullRequestDiffView diff, ThreadsView threads, PendingReviewView pending) {
        return shellReviewing(diff, threads, pending, mock(PendingReviewCommand.class));
    }

    static Shell shellReviewing(PullRequestDiffView diff, ThreadsView threads, PendingReviewView pending, PendingReviewCommand writing) {
        return shellReviewing(diff, threads, pending, writing, Runnable::run);
    }

    static Shell shellReviewing(
            PullRequestDiffView diff, ThreadsView threads, PendingReviewView pending, PendingReviewCommand writing, Executor background) {
        Shell shell = shellOnATicketWith(
                openPullRequestsViewOf(pullRequestView()), reviewsAnswering(diff, threads, pending), writing, background);
        shell.press(key('p'));
        return shell;
    }

    static Shell shellWriting(PendingReviewCommand writing) {
        return shellReviewing(twoFileDiffView(), readThreadsViewOf(), noPendingReviewView(), writing);
    }

    static Shell shellOnTheAddedLineWriting(PendingReviewCommand writing) {
        return ShellFixture.pressed(shellWriting(writing), key('j'), 2);
    }

    static PendingReviewCommand writingThatWrites(PendingReviewWriteView answer) {
        PendingReviewCommand writing = mock(PendingReviewCommand.class);
        when(writing.comment(any())).thenReturn(answer);
        when(writing.reply(any())).thenReturn(answer);
        when(writing.edit(any())).thenReturn(answer);
        when(writing.delete(any())).thenReturn(answer);
        when(writing.submit(any())).thenReturn(answer);
        return writing;
    }

    static PendingCommentView firstOfTwoPendingComments() {
        return pendingCommentView(pendingCommentId(), new CommentBody("first"), onDiffAnchorView(diffPath(), rightOn(3)));
    }

    static PendingCommentView secondOfTwoPendingComments() {
        return pendingCommentView(secondPendingCommentId(), new CommentBody("second"), onDiffAnchorView(diffPath(), rightOn(3)));
    }

    static Shell shellOnALineWithTwoPendingComments(PendingReviewCommand writing) {
        return ShellFixture.pressed(
                shellReviewing(
                        twoFileDiffView(),
                        readThreadsViewOf(),
                        openPendingViewOf(firstOfTwoPendingComments(), secondOfTwoPendingComments()),
                        writing),
                key('j'),
                2);
    }

    static Shell shellInCommentsOn(ReviewThreadView thread, PendingReviewCommand writing) {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf(thread), noPendingReviewView(), writing);
        shell.press(key('h'));
        shell.press(alt('j'));
        return shell;
    }

    static Shell typed(Shell shell, String text) {
        text.chars().forEach(character -> shell.press(character == '\n' ? ShellFixture.enter() : key((char) character)));
        return shell;
    }

    static ReadThreadsView threadsOnTwoFiles() {
        return readThreadsViewOf(
                openThreadView(diffPath(), rightOn(3)),
                outdatedThreadView(diffPath()),
                resolvedThreadView(secondDiffPath(), rightOn(1)));
    }

    static Shell shellReviewingWhileTheReadsHang(PullRequestDiffView diff, ThreadsView threads) {
        Shell shell = shellOnATicketWith(
                openPullRequestsViewOf(pullRequestView()),
                reviewsAnswering(diff, threads, noPendingReviewView()),
                mock(PendingReviewCommand.class),
                new HeldTasksFixture());
        shell.press(key('p'));
        return shell;
    }

    static OpenPendingReviewView pendingOnBothFiles() {
        return openPendingViewOf(
                commentOnView(diffPath(), rightOn(3)), replyViewOn(secondDiffPath(), rightOn(1)), commentOnView(diffPath(), rightOn(3)));
    }

    static String cursorText(Shell shell) {
        Screen screen = Screen.of(shell);
        return IntStream.range(0, screen.lines().size())
                .mapToObj(screen::reversedTextOnLine)
                .filter(text -> text.startsWith("▌"))
                .findFirst()
                .orElse("");
    }
}

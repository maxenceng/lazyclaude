package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.resolvedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.openPullRequestsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.pullRequestView;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReadThreadsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.OutsideSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;

final class ShellReviewFixture {

    private ShellReviewFixture() {}

    static ReviewQuery reviewsAnswering(PullRequestDiffView diff, ThreadsView threads) {
        ReviewQuery reviews = mock(ReviewQuery.class);
        when(reviews.loadingDiff()).thenReturn(loadingDiffView());
        when(reviews.loadingThreads()).thenReturn(loadingThreadsView());
        when(reviews.diffOf(any())).thenReturn(diff);
        when(reviews.threadsOf(any())).thenReturn(threads);
        return reviews;
    }

    static Shell shellOnATicketWith(PullRequestsView pullRequests, ReviewQuery reviews, Executor background) {
        Shell shell = new Shell(
                boardViewOf(slugTicketView()),
                emptyDocsView(),
                emptySessionsView(),
                pullRequests,
                mock(HostedSessionCommand.class, ShellFixture::noHostStarted),
                mock(WatchedSessionCommand.class),
                mock(OutsideSessionCommand.class),
                mock(TicketWorktreeCommand.class),
                blankScreens(),
                mock(BoardQuery.class),
                mock(DocsQuery.class),
                reviews,
                background);
        shell.press(key('j'));
        return shell;
    }

    static Shell shellOnATicketWithAnOpenPullRequest(ReviewQuery reviews, Executor background) {
        return shellOnATicketWith(openPullRequestsViewOf(pullRequestView()), reviews, background);
    }

    static Shell shellReviewing(PullRequestDiffView diff, ThreadsView threads) {
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviewsAnswering(diff, threads), Runnable::run);
        shell.press(key('p'));
        return shell;
    }

    static ReadThreadsView threadsOnTwoFiles() {
        return readThreadsViewOf(
                openThreadView(diffPath(), rightOn(3)),
                outdatedThreadView(diffPath()),
                resolvedThreadView(secondDiffPath(), rightOn(1)));
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

package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.addedFileView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.changedFileView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readDiffViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.renamedFileView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestTitle;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffPort;
import dev.maxenceng.lazyclaude.review.domain.ThreadPort;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.DiffFileView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.HunkView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQueryFixture;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.DiffLineFixture;
import dev.maxenceng.lazyclaude.review.published.DiffPathFixture;

final class ReviewPaneFixture {

    private static final int DRAWN_WIDTH = 80;

    private ReviewPaneFixture() {}

    static Screen drawn(ReviewPane pane, int height) {
        return Screen.of(new DiffElement(pane), DRAWN_WIDTH, height);
    }

    static ReviewQuery joiningReviews() {
        return ReviewQueryFixture.reviewQuery(mock(PullRequestDiffPort.class), mock(ThreadPort.class));
    }

    static DiffRows rowsOf(PullRequestDiffView diff, ThreadsView threads, boolean showResolved) {
        return DiffRows.of(diff, joiningReviews().threadsInDiff(diff, threads), showResolved);
    }

    static ReviewPane paneOver(PullRequestDiffView diff, ThreadsView threads) {
        return new ReviewPane(pullRequestNumber(), pullRequestTitle(), diff, threads, noPendingReviewView(), joiningReviews());
    }

    static ReviewPane loadingPane() {
        return paneOver(loadingDiffView(), loadingThreadsView());
    }

    static ReviewPane paneOverTwoFiles() {
        return paneOver(twoFileDiffView(), loadingThreadsView());
    }

    static ReviewPane paneOverFilesWithAPlainRename() {
        return paneOver(readDiffViewOf(changedFileView(), renamedFileView(), addedFileView()), loadingThreadsView());
    }

    static PullRequestDiffView diffOfLines(int lines) {
        List<DiffLine> added = IntStream.rangeClosed(1, lines)
                .mapToObj(number -> (DiffLine) DiffLineFixture.added("line " + number, number))
                .toList();
        HunkView hunk = PullRequestDiffViewFixture.hunkView("@@ -0,0 +1," + lines + " @@");
        DiffFileView file = PullRequestDiffViewFixture.fileView(
                DiffPathFixture.diffPath(),
                new dev.maxenceng.lazyclaude.review.published.AddedFile(),
                HunkView.builder().header(hunk.header()).lines(added).build());
        return readDiffViewOf(file);
    }
}

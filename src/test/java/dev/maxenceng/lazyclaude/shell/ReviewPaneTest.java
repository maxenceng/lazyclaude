package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.addedFileView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readDiffViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.resolvedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableDiffView;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.diffOfLines;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.loadingPane;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.paneOver;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.paneOverFilesWithAPlainRename;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.paneOverTwoFiles;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ReviewPaneTest {

    @Test
    void shouldStartTheCursorOnTheFirstLineOfTheDiff() {
        assertThat(paneOverTwoFiles().cursor()).isZero();
    }

    @Test
    void shouldMoveTheCursorOneLineAtATimeAndStopAtTheLastLine() {
        ReviewPane pane = paneOverTwoFiles();

        pane.lineDown();
        assertThat(pane.cursor()).isEqualTo(1);
        IntStream.range(0, 10).forEach(_ -> pane.lineDown());

        assertThat(pane.cursor()).isEqualTo(5);
    }

    @Test
    void shouldStopTheCursorOnTheFirstLineWhenMovingUpFromIt() {
        ReviewPane pane = paneOverTwoFiles();

        pane.lineUp();

        assertThat(pane.cursor()).isZero();
    }

    @Test
    void shouldMoveHalfAPageOfTheHeightLastDrawnAndNeverBeyondTheEnds() {
        ReviewPane pane = paneOver(diffOfLines(100), loadingThreadsView());
        pane.observeHeight(20);

        pane.halfPageDown();
        assertThat(pane.cursor()).isEqualTo(10);
        pane.halfPageUp();
        pane.halfPageUp();

        assertThat(pane.cursor()).isZero();
    }

    @Test
    void shouldMoveAtLeastOneLineForAViewOnlyOneRowHigh() {
        ReviewPane pane = paneOverTwoFiles();
        pane.observeHeight(1);

        pane.halfPageDown();

        assertThat(pane.cursor()).isEqualTo(1);
    }

    @Test
    void shouldJumpToTheFirstLineOfTheNextFileAndStayPutOnTheLastFile() {
        ReviewPane pane = paneOverTwoFiles();

        pane.nextFile();
        assertThat(pane.cursor()).isEqualTo(4);
        pane.nextFile();

        assertThat(pane.cursor()).as("there is no file after the last").isEqualTo(4);
    }

    @Test
    void shouldJumpToTheFirstLineOfThePreviousFileAndStayPutOnTheFirstFile() {
        ReviewPane pane = paneOverTwoFiles();
        pane.nextFile();

        pane.previousFile();
        assertThat(pane.cursor()).isZero();
        pane.lineDown();
        pane.previousFile();

        assertThat(pane.cursor()).as("there is no file before the first").isEqualTo(1);
    }

    @Test
    void shouldStepOverAFileWithoutLinesWhenJumpingBetweenFiles() {
        ReviewPane pane = paneOverFilesWithAPlainRename();

        pane.nextFile();

        assertThat(pane.cursor()).isEqualTo(4);
    }

    @Test
    void shouldHaveTheFilesSelectionFollowTheCursorsFile() {
        ReviewPane pane = paneOverTwoFiles();

        pane.nextFile();

        assertThat(pane.selectedFile()).isEqualTo(1);
        pane.previousFile();
        assertThat(pane.selectedFile()).isZero();
    }

    @Test
    void shouldMoveTheFilesSelectionWithoutMovingTheCursorAndStopAtTheEnds() {
        ReviewPane pane = paneOverTwoFiles();

        pane.selectFileUp();
        assertThat(pane.selectedFile()).isZero();
        pane.selectFileDown();
        pane.selectFileDown();

        assertThat(pane.selectedFile()).isEqualTo(1);
        assertThat(pane.cursor()).isZero();
    }

    @Test
    void shouldTakeTheCursorToTheFirstLineOfTheSelectedFile() {
        ReviewPane pane = paneOverTwoFiles();
        pane.selectFileDown();

        pane.goToSelectedFile();

        assertThat(pane.cursor()).isEqualTo(4);
    }

    @Test
    void shouldLeaveTheCursorWhereItIsWhenTheSelectedFileHasNoLines() {
        ReviewPane pane = paneOverFilesWithAPlainRename();
        pane.selectFileDown();

        pane.goToSelectedFile();

        assertThat(pane.cursor()).isZero();
    }

    @Test
    void shouldTakeTheCursorToTheLineOfTheSelectedThread() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(openThreadView(diffPath(), rightOn(3))));

        pane.goToSelectedComment();

        assertThat(pane.cursor()).isEqualTo(2);
        assertThat(pane.selectedFile()).isZero();
    }

    @Test
    void shouldLeaveTheCursorWhereItIsForAnOutdatedThread() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(outdatedThreadView(diffPath())));
        pane.lineDown();

        pane.goToSelectedComment();

        assertThat(pane.cursor()).isEqualTo(1);
    }

    @Test
    void shouldMoveTheCommentsSelectionAndStopAtTheEnds() {
        ReviewPane pane = paneOver(
                twoFileDiffView(),
                readThreadsViewOf(openThreadView(diffPath(), rightOn(3)), openThreadView(secondDiffPath(), rightOn(1))));

        pane.selectCommentUp();
        assertThat(pane.selectedComment()).isZero();
        pane.selectCommentDown();
        pane.selectCommentDown();

        assertThat(pane.selectedComment()).isEqualTo(1);
    }

    @Test
    void shouldListNoCommentsUntilTheThreadsAreRead() {
        assertThat(paneOverTwoFiles().comments()).isEmpty();
    }

    @Test
    void shouldKeepTheCursorOnItsLineWhenResolvedThreadsAreShownOrHiddenAgain() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(resolvedThreadView(diffPath(), rightOn(3))));
        pane.nextFile();

        pane.toggleResolved();

        assertThat(pane.showResolved()).isTrue();
        assertThat(pane.cursor()).isEqualTo(4);
        assertThat(pane.rows().size()).isEqualTo(11);
        pane.toggleResolved();
        assertThat(pane.rows().size()).isEqualTo(10);
    }

    @Test
    void shouldCountTheOpenThreadsOfAFileButNotTheResolvedOrOutdatedOnes() {
        ReviewPane pane = paneOver(
                twoFileDiffView(),
                readThreadsViewOf(
                        openThreadView(diffPath(), rightOn(3)),
                        openThreadView(diffPath(), rightOn(2)),
                        resolvedThreadView(diffPath(), rightOn(4)),
                        outdatedThreadView(diffPath()),
                        openThreadView(secondDiffPath(), rightOn(1))));

        assertThat(pane.openThreadsIn(diffPath())).isEqualTo(2);
        assertThat(pane.openThreadsIn(secondDiffPath())).isEqualTo(1);
    }

    @Test
    void shouldMoveNothingWhileTheDiffIsNotRead() {
        ReviewPane pane = loadingPane();

        pane.lineDown();
        pane.nextFile();
        pane.halfPageDown();
        pane.goToSelectedFile();

        assertThat(pane.cursor()).isZero();
    }

    @Test
    void shouldTakeTheLatestReadOfTheDiffAndDropAnEarlierOneThatLandsAfterIt() {
        ReviewPane pane = loadingPane();
        long first = pane.beginDiffRead();
        long second = pane.beginDiffRead();

        pane.applyDiff(second, twoFileDiffView());
        pane.applyDiff(first, unavailableDiffView());

        assertThat(pane.diff()).isEqualTo(twoFileDiffView());
    }

    @Test
    void shouldTakeTheLatestReadOfTheThreadsAndDropAnEarlierOneThatLandsAfterIt() {
        ReviewPane pane = loadingPane();
        long first = pane.beginThreadsRead();
        long second = pane.beginThreadsRead();

        pane.applyThreads(second, readThreadsViewOf(openThreadView(diffPath(), rightOn(3))));
        pane.applyThreads(first, loadingThreadsView());

        assertThat(pane.comments()).hasSize(1);
    }

    @Test
    void shouldPullTheCursorBackWhenARereadDiffHasFewerLines() {
        ReviewPane pane = paneOverTwoFiles();
        pane.nextFile();
        pane.lineDown();

        pane.applyDiff(pane.beginDiffRead(), readDiffViewOf(addedFileView()));

        assertThat(pane.cursor()).isEqualTo(1);
        assertThat(pane.selectedFile()).isZero();
    }

    @Test
    void shouldReplaceTheDiffWithTheReasonWhenARereadFails() {
        ReviewPane pane = paneOverTwoFiles();

        pane.applyDiff(pane.beginDiffRead(), unavailableDiffView());

        assertThat(pane.diff()).isEqualTo(unavailableDiffView());
        assertThat(pane.rows().size()).as("the last good diff is never shown as current").isZero();
    }

    @Test
    void shouldStartLoading() {
        assertThat(loadingPane().diff()).isEqualTo(loadingDiffView());
    }
}

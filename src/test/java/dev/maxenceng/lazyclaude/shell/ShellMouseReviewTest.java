package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.openPendingViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.writtenView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.escape;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.leftPress;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.cursorText;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.pendingOnBothFiles;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellOnALongTicketWithAPullRequest;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellReviewing;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellWriting;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.threadsOnTwoFiles;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.typed;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.writingThatWrites;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.tamboui.layout.Position;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

class ShellMouseReviewTest {

    @Test
    void shouldFocusFilesAndSelectTheRowClicked() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf());

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("A Docs.java")));

        assertThat(shell.focus()).isEqualTo(Focus.FILES);
        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("A Docs.java"));
    }

    @Test
    void shouldOnlySelectTheFileOnTheFirstClickAndGoToItsLineOnTheSecond() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf());
        Position docs = Screen.of(shell).positionInLeftColumn("A Docs.java");

        shell.mouse(leftPress(docs));
        assertThat(cursorText(shell)).contains("keep");
        shell.mouse(leftPress(docs));

        assertThat(shell.focus()).isEqualTo(Focus.FILES);
        assertThat(cursorText(shell)).contains("+ first");
    }

    @Test
    void shouldFocusCommentsAndSelectTheRowClicked() {
        Shell shell = shellReviewing(twoFileDiffView(), threadsOnTwoFiles());

        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("Docs.java:1 alice")));

        assertThat(shell.focus()).isEqualTo(Focus.COMMENTS);
        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("Docs.java:1 alice"));
    }

    @Test
    void shouldGoToAThreadsLineOnASecondClickOnItsRow() {
        Shell shell = shellReviewing(twoFileDiffView(), threadsOnTwoFiles());
        Position thread = Screen.of(shell).positionInLeftColumn("Docs.java:1 alice");
        shell.mouse(leftPress(thread));

        shell.mouse(leftPress(thread));

        assertThat(cursorText(shell)).contains("+ first");
    }

    @Test
    void shouldOpenAPendingCommentInTheBoxOnASecondClickOnItsRow() {
        Shell shell = shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), pendingOnBothFiles());
        Position pending = Screen.of(shell).positionInLeftColumn("✎ Docs.java:1");
        shell.mouse(leftPress(pending));
        assertThat(Screen.of(shell).text()).doesNotContain("^s save");

        shell.mouse(leftPress(pending));

        assertThat(Screen.of(shell).text()).contains("^s save");
    }

    @Test
    void shouldMoveTheFilesSelectionARowANotchAndFocusFiles() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf());
        Position files = Screen.of(shell).positionInLeftColumn("M Board.java");

        shell.mouse(MouseEvent.scrollDown(files.x(), files.y()));
        shell.mouse(MouseEvent.scrollDown(files.x(), files.y()));

        assertThat(shell.focus()).isEqualTo(Focus.FILES);
        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("A Docs.java"));
    }

    @Test
    void shouldMoveTheCommentsSelectionBackUpANotch() {
        Shell shell = shellReviewing(twoFileDiffView(), threadsOnTwoFiles());
        Position comments = Screen.of(shell).positionInLeftColumn("Docs.java:1 alice");
        shell.mouse(leftPress(comments));

        shell.mouse(MouseEvent.scrollUp(comments.x(), comments.y()));

        assertThat(shell.focus()).isEqualTo(Focus.COMMENTS);
        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("Board.java:3 alice"));
    }

    @Test
    void shouldFocusTheDiffAndMoveTheCursorToTheLineClicked() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf());
        shell.press(key('h'));

        shell.mouse(leftPress(Screen.of(shell).positionOf("+ new")));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(cursorText(shell)).contains("+ new");
    }

    @Test
    void shouldEditTheLinesPendingCommentOnASecondClickOnTheLine() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf(), pendingOnBothFiles());
        Position line = Screen.of(shell).positionOf("+ new");
        shell.mouse(leftPress(line));
        assertThat(Screen.of(shell).text()).doesNotContain("^s save");

        shell.mouse(leftPress(line));

        assertThat(Screen.of(shell).text()).contains("^s save");
    }

    @Test
    void shouldMoveTheCursorThreeLinesPerWheelNotchOverTheDiffAndFocusIt() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf());
        shell.press(key('h'));
        Position diff = Screen.of(shell).positionOf("+ new");

        shell.mouse(MouseEvent.scrollDown(diff.x(), diff.y()));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(cursorText(shell)).contains("end");
    }

    @Test
    void shouldMoveTheCursorBackUpThreeLinesPerWheelNotch() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf());
        Position diff = Screen.of(shell).positionOf("+ new");
        shell.mouse(MouseEvent.scrollDown(diff.x(), diff.y()));

        shell.mouse(MouseEvent.scrollUp(diff.x(), diff.y()));

        assertThat(cursorText(shell)).contains("keep");
    }

    @Test
    void shouldCommentOnTheRangeADragSelectsFromThePressLineToTheReleaseLine() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = shellWriting(writing);
        Position from = Screen.of(shell).positionOf("+ new");
        Position to = Screen.of(shell).positionOf("4    4   end");

        shell.mouse(leftPress(from));
        shell.mouse(MouseEvent.drag(MouseButton.LEFT, to.x(), to.y()));
        shell.mouse(MouseEvent.release(MouseButton.LEFT, to.x(), to.y()));
        shell.press(key('c'));
        typed(shell, "range");
        shell.press(ctrl('s'));

        verify(writing).comment(new PendingCommentPayload(
                project().root(), pullRequestNumber(), diffPath(), rightRange(3, 4), new CommentBody("range")));
    }

    @Test
    void shouldClampADragReleasedAboveTheDiffToItsFirstLine() {
        Shell shell = shellWriting(mock(PendingReviewCommand.class));
        Position from = Screen.of(shell).positionOf("4    4   end");

        shell.mouse(leftPress(from));
        shell.mouse(MouseEvent.release(MouseButton.LEFT, from.x(), 0));

        assertThat(cursorText(shell)).contains("keep");
        assertThat(Screen.of(shell).lines().getLast()).contains("a range must stay on one side of the diff");
        assertThat(Screen.of(shell).text()).as("a refused range leaves no range marked").doesNotContain("┃");
    }

    @Test
    void shouldClampADragReleasedBelowTheDiffToItsLastLine() {
        Shell shell = shellWriting(mock(PendingReviewCommand.class));
        Position from = Screen.of(shell).positionOf("- old");

        shell.mouse(leftPress(from));
        shell.mouse(MouseEvent.release(MouseButton.LEFT, from.x(), Screen.of(shell).lines().size() - 1));

        assertThat(Screen.of(shell).lines().getLast()).contains("a range must stay inside one hunk");
    }

    @Test
    void shouldMarkNoRangeForAPressAndReleaseOnTheSameLine() {
        Shell shell = shellWriting(mock(PendingReviewCommand.class));
        Position line = Screen.of(shell).positionOf("+ new");

        shell.mouse(leftPress(line));
        shell.mouse(MouseEvent.release(MouseButton.LEFT, line.x(), line.y()));

        assertThat(Screen.of(shell).text()).doesNotContain("┃");
    }

    @Test
    void shouldEndADragAtTheNextPressSoAReleaseNeverReportedDoesNotSelectARange() {
        Shell shell = shellWriting(mock(PendingReviewCommand.class));
        shell.mouse(leftPress(Screen.of(shell).positionOf("- old")));
        shell.mouse(leftPress(Screen.of(shell).positionInLeftColumn("A Docs.java")));
        Position end = Screen.of(shell).positionOf("4    4   end");

        shell.mouse(MouseEvent.release(MouseButton.LEFT, end.x(), end.y()));

        assertThat(Screen.of(shell).text()).doesNotContain("┃");
    }

    @Test
    void shouldFocusNothingAndSubmitNothingOnAClickOnTheReviewPanel() {
        PendingReviewCommand writing = mock(PendingReviewCommand.class);
        Shell shell = shellWriting(writing);
        Position panel = Screen.of(shell).positionInLeftColumn("no pending review");

        assertThat(shell.mouse(leftPress(panel))).isFalse();
        shell.mouse(leftPress(panel));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).text()).doesNotContain("submit review", "^s save");
        verifyNoInteractions(writing);
    }

    @Test
    void shouldTakeEveryClickWhileTheCommentBoxIsOpenAndChangeNothing() {
        Shell shell = shellWriting(mock(PendingReviewCommand.class));
        shell.press(key('S'));
        Position docs = Screen.of(shell).positionInLeftColumn("A Docs.java");

        assertThat(shell.mouse(leftPress(docs))).isTrue();

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).text()).contains("^s save");
    }

    @Test
    void shouldLeaveTheTicketDetailUnscrolledByAWheelOverTheDiffNotice() {
        Shell wheeled = shellOnALongTicketWithAPullRequest(loadingDiffView(), readThreadsViewOf());
        Shell untouched = shellOnALongTicketWithAPullRequest(loadingDiffView(), readThreadsViewOf());
        Screen.of(wheeled);
        Screen.of(untouched);
        wheeled.press(key('p'));
        untouched.press(key('p'));
        Position notice = Screen.of(wheeled).positionOf("loading");

        wheeled.mouse(MouseEvent.scrollDown(notice.x(), notice.y()));
        wheeled.press(escape());
        untouched.press(escape());

        assertThat(Screen.of(wheeled).mainViewRows())
                .as("the wheel over a review's notice reaches no view the review hides")
                .isEqualTo(Screen.of(untouched).mainViewRows());
    }

    @Test
    void shouldFocusCommentsOnAClickOrAWheelOverItsNotice() {
        Shell clicked = shellReviewing(twoFileDiffView(), loadingThreadsView());
        Shell wheeled = shellReviewing(twoFileDiffView(), loadingThreadsView());
        Position notice = Screen.of(clicked).positionInLeftColumn("loading");
        Screen.of(wheeled);

        clicked.mouse(leftPress(notice));
        wheeled.mouse(MouseEvent.scrollDown(notice.x(), notice.y()));

        assertThat(clicked.focus()).isEqualTo(Focus.COMMENTS);
        assertThat(wheeled.focus()).isEqualTo(Focus.COMMENTS);
    }

    @Test
    void shouldKeepThePressLineOfADragThroughAWheelNotch() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = shellWriting(writing);
        Position from = Screen.of(shell).positionOf("+ new");
        Position to = Screen.of(shell).positionOf("4    4   end");

        shell.mouse(leftPress(from));
        shell.mouse(MouseEvent.scrollDown(from.x(), from.y()));
        Screen.of(shell);
        shell.mouse(MouseEvent.release(MouseButton.LEFT, to.x(), to.y()));
        shell.press(key('c'));
        typed(shell, "range");
        shell.press(ctrl('s'));

        verify(writing).comment(new PendingCommentPayload(
                project().root(), pullRequestNumber(), diffPath(), rightRange(3, 4), new CommentBody("range")));
    }
}

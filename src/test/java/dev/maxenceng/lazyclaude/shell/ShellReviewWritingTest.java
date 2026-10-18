package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.commentOnView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.openPendingViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.refusedWriteView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.replyViewOn;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.writtenView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.editedCommentBody;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.secondPendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.firstOfTwoPendingComments;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellInCommentsOn;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellOnALineWithTwoPendingComments;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellOnTheAddedLineWriting;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellReviewing;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellWriting;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.typed;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.writingThatWrites;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingDeletePayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingEditPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReplyPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.tamboui.tui.event.KeyCode;

class ShellReviewWritingTest {

    @Test
    void shouldOpenTheCommentBoxUnderTheCursorsLineOnCWithItsHintAndTheCursorInsideIt() {
        Shell shell = shellOnTheAddedLineWriting(mock(PendingReviewCommand.class));

        shell.press(key('c'));
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows()).satisfiesExactly(
                row -> assertThat(row).startsWith("── src/Board.java"),
                row -> assertThat(row).startsWith("@@"),
                row -> assertThat(row).endsWith("keep"),
                row -> assertThat(row).endsWith("- old"),
                row -> assertThat(row).endsWith("3 + new"),
                row -> assertThat(row.strip()).isEqualTo("╭─────────────────────────────────────╮"),
                row -> assertThat(row.strip()).startsWith("│ "),
                row -> assertThat(row.strip()).startsWith("╰ ^s save  esc cancel ─"),
                row -> assertThat(row).endsWith("end"));
        assertThat(screen.text()).contains("╰ ^s save  esc cancel ─");
        assertThat(screen.cursor()).isPresent();
        assertThat(screen.lines().get(screen.cursor().get().y())).contains("│");
    }

    @Test
    void shouldTypeIntoTheBoxAndTakeEnterAsANewline() {
        Shell shell = shellOnTheAddedLineWriting(mock(PendingReviewCommand.class));

        typed(shell, "first");
        shell.press(key('c'));
        typed(shell, "ab\ncd");
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows())
                .anySatisfy(row -> assertThat(row.strip()).startsWith("│ ab "))
                .anySatisfy(row -> assertThat(row.strip()).startsWith("│ cd "));
        assertThat(screen.text()).doesNotContain("first");
    }

    @Test
    void shouldThrowTheTextAwayOnEscapeAndKeepTheReviewOpen() {
        Shell shell = shellOnTheAddedLineWriting(mock(PendingReviewCommand.class));
        shell.press(key('c'));
        typed(shell, "lost");

        shell.press(namedKey(KeyCode.ESCAPE));
        Screen screen = Screen.of(shell);

        assertThat(screen.text()).doesNotContain("lost", "esc cancel").contains("╭Files");
        shell.press(key('c'));
        assertThat(Screen.of(shell).text()).doesNotContain("lost");
    }

    @Test
    void shouldDoNothingOnCWhileThereIsNoLineToComment() {
        Shell shell = shellReviewing(loadingDiffView(), loadingThreadsView(), noPendingReviewView());

        shell.press(key('c'));

        assertThat(Screen.of(shell).text()).doesNotContain("esc cancel");
    }

    @Test
    void shouldSaveOnCtrlSAsANewCommentOnTheNewSideOfAnAddedLine() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf(commentOnView(diffPath(), rightOn(3)))));
        Shell shell = shellOnTheAddedLineWriting(writing);
        shell.press(key('c'));
        typed(shell, "Does this need its own Element?");

        shell.press(ctrl('s'));

        verify(writing).comment(new PendingCommentPayload(
                project().root(), pullRequestNumber(), diffPath(), rightOn(3), commentBody()));
    }

    @Test
    void shouldSaveACommentOnARemovedLineOnTheOldSideAtItsOldNumber() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = ShellFixture.pressed(shellWriting(writing), key('j'), 1);
        shell.press(key('c'));
        typed(shell, "Does this need its own Element?");

        shell.press(ctrl('s'));

        verify(writing).comment(new PendingCommentPayload(
                project().root(), pullRequestNumber(), diffPath(), leftOn(3), commentBody()));
    }

    @Test
    void shouldCloseTheBoxAndShowTheCommentWithItsMarkOnlyOnceGitHubHasConfirmedIt() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf(commentOnView(diffPath(), rightOn(3)))));
        Shell shell = shellOnTheAddedLineWriting(writing);
        shell.press(key('c'));
        typed(shell, "Does this need its own Element?");
        assertThat(Screen.of(shell).text()).doesNotContain("✎");

        shell.press(ctrl('s'));
        Screen screen = Screen.of(shell);

        assertThat(screen.text()).doesNotContain("esc cancel");
        assertThat(screen.mainViewRows().stream().map(String::strip).toList()).contains("✎ L3 you");
        assertThat(screen.boardPanelLines()).anySatisfy(row -> assertThat(row).endsWith("✎1"));
    }

    @Test
    void shouldKeepTheBoxOpenWithItsTextAndWhyWhenTheSaveIsRefusedAndSaveAgainOnCtrlS() {
        PendingReviewCommand writing = writingThatWrites(refusedWriteView());
        Shell shell = shellOnTheAddedLineWriting(writing);
        shell.press(key('c'));
        typed(shell, "mine");

        shell.press(ctrl('s'));
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows())
                .anySatisfy(row -> assertThat(row.strip()).startsWith("│ mine "))
                .anySatisfy(row -> assertThat(row.strip()).isEqualTo("not saved (gh: not logged in)"));
        assertThat(screen.dimText()).anySatisfy(text -> assertThat(text).contains("not saved (gh: not logged in)"));
        shell.press(ctrl('s'));
        verify(writing, times(2)).comment(any());
    }

    @Test
    void shouldShowSavingDimUnderTheBoxWhileTheSaveIsOutAndHoldTheTextStill() {
        HoldableTasksFixture background = new HoldableTasksFixture();
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf(commentOnView(diffPath(), rightOn(3)))));
        Shell shell = ShellFixture.pressed(
                shellReviewing(twoFileDiffView(), readThreadsViewOf(), noPendingReviewView(), writing, background), key('j'), 2);
        shell.press(key('c'));
        typed(shell, "mine");
        background.startHolding();

        shell.press(ctrl('s'));
        typed(shell, "more");
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows())
                .anySatisfy(row -> assertThat(row.strip()).startsWith("│ mine "))
                .anySatisfy(row -> assertThat(row.strip()).isEqualTo("saving…"));
        assertThat(screen.dimText()).anySatisfy(text -> assertThat(text).contains("saving…"));
        assertThat(screen.text()).doesNotContain("more", "✎");
        verify(writing, never()).comment(any());
        background.releaseInOrder();
        assertThat(Screen.of(shell).text()).doesNotContain("saving…");
    }

    @Test
    void shouldCommentOnARangeStartedAndEndedWithVAsFromItsFirstLineToItsLast() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = shellOnTheAddedLineWriting(writing);

        shell.press(key('v'));
        shell.press(key('j'));
        shell.press(key('v'));
        shell.press(key('c'));
        typed(shell, "range");
        shell.press(ctrl('s'));

        verify(writing).comment(new PendingCommentPayload(
                project().root(), pullRequestNumber(), diffPath(), rightRange(3, 4), new CommentBody("range")));
    }

    @Test
    void shouldMarkTheLinesOfTheRangeInTheGutterWhileItIsMarked() {
        Shell shell = shellOnTheAddedLineWriting(mock(PendingReviewCommand.class));

        shell.press(key('v'));
        shell.press(key('j'));
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows()).anySatisfy(row -> assertThat(row).startsWith("┃").endsWith("3 + new"));
    }

    @Test
    void shouldRefuseARangeThatLeavesItsHunkWithAMessageAndLeaveTheRangeAsItWas() {
        PendingReviewCommand writing = mock(PendingReviewCommand.class);
        Shell shell = shellOnTheAddedLineWriting(writing);
        shell.press(key('v'));
        ShellFixture.pressed(shell, key('j'), 2);

        shell.press(key('v'));

        assertThat(Screen.of(shell).lines().getLast()).contains("a range must stay inside one hunk");
        shell.press(key('c'));
        assertThat(Screen.of(shell).text()).doesNotContain("esc cancel");
        assertThat(Screen.of(shell).lines().getLast()).contains("a range must stay inside one hunk");
    }

    @Test
    void shouldAcceptARangeWhoseLastLineIsTheLastLineOfItsHunk() {
        Shell shell = shellOnTheAddedLineWriting(mock(PendingReviewCommand.class));
        shell.press(key('v'));
        shell.press(key('j'));
        shell.press(key('v'));

        shell.press(key('c'));

        assertThat(Screen.of(shell).text()).contains("esc cancel");
    }

    @Test
    void shouldClearTheRangeOnEscapeBeforeLeavingTheReview() {
        Shell shell = shellOnTheAddedLineWriting(mock(PendingReviewCommand.class));
        shell.press(key('v'));

        shell.press(namedKey(KeyCode.ESCAPE));

        assertThat(Screen.of(shell).text()).contains("╭Files").doesNotContain("┃");
        shell.press(namedKey(KeyCode.ESCAPE));
        assertThat(Screen.of(shell).text()).contains("╭Board");
    }

    @Test
    void shouldOpenTheLastPendingCommentOfALineWithAMarkInTheBoxOnEnterAndSaveItsNewTextToIt() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = ShellFixture.pressed(shellOnALineWithTwoPendingComments(writing), enter(), 1);

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row.strip()).startsWith("│ second "));
        typed(shell, "!");
        shell.press(ctrl('s'));

        verify(writing).edit(new PendingEditPayload(project().root(), pullRequestNumber(), secondPendingCommentId(), new CommentBody("second!")));
    }

    @Test
    void shouldSaveAnEmptiedPendingCommentThroughEditSoThatItIsDeleted() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = ShellFixture.pressed(shellOnALineWithTwoPendingComments(writing), enter(), 1);
        ShellFixture.pressed(shell, namedKey(KeyCode.BACKSPACE), "second".length());

        shell.press(ctrl('s'));

        verify(writing).edit(new PendingEditPayload(project().root(), pullRequestNumber(), secondPendingCommentId(), new CommentBody("")));
    }

    @Test
    void shouldDeleteTheLastPendingCommentOfALineOnXAndShowWhatGitHubLeft() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf(firstOfTwoPendingComments())));
        Shell shell = shellOnALineWithTwoPendingComments(writing);

        shell.press(key('x'));

        verify(writing).delete(new PendingDeletePayload(project().root(), pullRequestNumber(), secondPendingCommentId()));
        assertThat(Screen.of(shell).mainViewRows().stream().map(String::strip).toList()).contains("first").doesNotContain("second");
    }

    @Test
    void shouldSayWhyAPendingCommentWasNotDeletedWhenGitHubRefusesAndKeepIt() {
        PendingReviewCommand writing = writingThatWrites(refusedWriteView());
        Shell shell = shellOnALineWithTwoPendingComments(writing);

        shell.press(key('x'));
        Screen screen = Screen.of(shell);

        assertThat(screen.lines().getLast()).contains("not deleted (gh: not logged in)");
        assertThat(screen.mainViewRows().stream().map(String::strip).toList()).contains("second");
    }

    @Test
    void shouldDoNothingOnEnterOrXOnALineWithoutAMark() {
        PendingReviewCommand writing = mock(PendingReviewCommand.class);
        Shell shell = ShellFixture.pressed(shellOnALineWithTwoPendingComments(writing), key('k'), 1);

        shell.press(enter());
        shell.press(key('x'));

        assertThat(Screen.of(shell).text()).doesNotContain("esc cancel");
        verifyNoInteractions(writing);
    }

    @Test
    void shouldOpenTheSelectedPendingCommentOfCommentsOnEnterAndDeleteItOnX() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = ShellFixture.pressed(shellOnALineWithTwoPendingComments(writing), key('h'), 1);
        shell.press(alt('j'));
        ShellFixture.pressed(shell, key('j'), 1);

        shell.press(key('x'));
        verify(writing).delete(new PendingDeletePayload(project().root(), pullRequestNumber(), secondPendingCommentId()));
        shell.press(enter());
        assertThat(Screen.of(shell).text()).doesNotContain("esc cancel");
    }

    @Test
    void shouldOpenAReplyBoxInlineUnderAPlacedThreadOnCInCommentsAndSaveItAsAReplyToIt() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf(replyViewOn(diffPath(), rightOn(3)))));
        Shell shell = shellInCommentsOn(openThreadView(diffPath(), rightOn(3)), writing);

        shell.press(key('c'));
        typed(shell, "It does, for the cursor.");
        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows().stream().map(String::strip).toList())
                .containsSubsequence("◆ L3 alice", "bob  It assumes markdown.", "╭─────────────────────────────────────╮");
        shell.press(ctrl('s'));
        verify(writing).reply(new PendingReplyPayload(project().root(), pullRequestNumber(), threadId(), editedCommentBody()));
        assertThat(Screen.of(shell).mainViewRows().stream().map(String::strip).toList()).contains("✎ reply you");
    }

    @Test
    void shouldOpenAReplyToAnOutdatedThreadInAPopupOverTheMainViewWithTheThreadDrawnAbove() {
        PendingReviewCommand writing = writingThatWrites(writtenView(openPendingViewOf()));
        Shell shell = shellInCommentsOn(outdatedThreadView(diffPath()), writing);

        shell.press(key('c'));
        typed(shell, "mine");
        Screen screen = Screen.of(shell);

        assertThat(screen.text()).contains("╭ Why not reuse it?");
        assertThat(screen.text()).contains("alice  Why not reuse it?");
        assertThat(screen.text()).contains("│ mine");
        assertThat(screen.text()).contains("^s save  esc cancel");
        assertThat(screen.cursor()).isPresent();
        shell.press(ctrl('s'));
        verify(writing).reply(new PendingReplyPayload(project().root(), pullRequestNumber(), threadId(), new CommentBody("mine")));
        assertThat(Screen.of(shell).text()).doesNotContain("^s save");
    }

    @Test
    void shouldDoNothingOnCInCommentsWhenThePendingCommentIsTheSelectedOne() {
        Shell shell = ShellFixture.pressed(shellOnALineWithTwoPendingComments(mock(PendingReviewCommand.class)), key('h'), 1);
        shell.press(alt('j'));

        shell.press(key('c'));

        assertThat(Screen.of(shell).text()).doesNotContain("esc cancel");
    }
}

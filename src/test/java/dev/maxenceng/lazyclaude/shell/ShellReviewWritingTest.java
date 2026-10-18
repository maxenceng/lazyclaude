package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.commentOnView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.openPendingViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.refusedWriteView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.writtenView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
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

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
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
}

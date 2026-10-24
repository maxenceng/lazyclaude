package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.fileWithOldNumbersFarAboveNewOnesView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readDiffViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableThreadsView;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.cursorText;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.reviewsAnswering;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellOnATicketWithAnOpenPullRequest;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellReviewing;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.threadsOnTwoFiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.tamboui.style.Color;

class ShellReviewKeysTest {

    @Test
    void shouldFocusTheMainViewOnPSoTheKeysMoveTheCursor() {
        assertThat(shellReviewing(twoFileDiffView(), loadingThreadsView()).focus()).isEqualTo(Focus.MAIN);
    }

    @Test
    void shouldListEachFileWithItsStatusLetterAndNameAndNoThreadMarkWhenItHasNone() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), readThreadsViewOf()));

        assertThat(screen.boardPanelLines()).anySatisfy(row -> assertThat(row).isEqualTo("M Board.java"));
        assertThat(screen.boardPanelLines()).anySatisfy(row -> assertThat(row).isEqualTo("A Docs.java"));
        assertThat(screen.text()).doesNotContain("◆");
    }

    @Test
    void shouldMarkAFileWithItsOpenThreadsAtTheRightEdgeNotCountingResolvedOrOutdatedOnes() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles()));

        assertThat(screen.boardPanelLines())
                .anySatisfy(row -> assertThat(row).startsWith("M Board.java").endsWith("◆1"))
                .anySatisfy(row -> assertThat(row).startsWith("A Docs.java").doesNotContain("◆"));
        assertThat(screen.foregroundTextOnLine(screen.lineOf("M Board.java"), Color.MAGENTA)).isEqualTo("◆1");
    }

    @Test
    void shouldColourTheStatusLetterOfEachFile() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), loadingThreadsView()));

        assertThat(screen.foregroundTextOnLine(screen.lineOf("M Board.java"), Color.YELLOW)).isEqualTo("M");
        assertThat(screen.foregroundTextOnLine(screen.lineOf("A Docs.java"), Color.GREEN)).isEqualTo("A");
    }

    @Test
    void shouldListEveryThreadInCommentsInDiffOrderWithOutdatedOnesMarkedAndResolvedOnesDim() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles()));

        assertThat(screen.boardPanelLines())
                .containsSubsequence("◆ Board.java:3 alice", "  Docs.java:1 alice", "  Board.java  outdated");
        assertThat(screen.dimText()).anySatisfy(text -> assertThat(text).contains("Docs.java:1 alice"));
        assertThat(screen.dimText()).noneSatisfy(text -> assertThat(text).contains("Board.java:3"));
    }

    @Test
    void shouldListThreadsInCommentsInDiffOrderEvenWhereTheirLineNumbersSayOtherwise() {
        Screen screen = Screen.of(shellReviewing(
                readDiffViewOf(fileWithOldNumbersFarAboveNewOnesView()),
                readThreadsViewOf(openThreadView(diffPath(), rightOn(50)), openThreadView(diffPath(), leftOn(100)))));

        assertThat(screen.boardPanelLines())
                .as("old line 100 is removed above where new line 50 is added, so its thread is listed first, marked as the old side")
                .containsSubsequence("◆ Board.java:-100 alice", "◆ Board.java:50 alice");
    }

    @Test
    void shouldStillListTheThreadsInCommentsWhenTheDiffCannotBeRead() {
        Screen screen = Screen.of(shellReviewing(unavailableDiffView(), threadsOnTwoFiles()));

        assertThat(screen.boardPanelLines()).anySatisfy(row -> assertThat(row).contains("Board.java:3 alice"));
    }

    @Test
    void shouldShowThreadsUnknownWithTheReasonInCommentsWhenTheyCannotBeReadWhileTheDiffStillShows() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), unavailableThreadsView()));

        assertThat(screen.boardPanelText()).contains("threads unknown (gh: not");
        assertThat(screen.dimText()).anySatisfy(text -> assertThat(text).contains("threads unknown"));
        assertThat(screen.mainViewRows()).anySatisfy(row -> assertThat(row).startsWith("── src/Board.java"));
    }

    @Test
    void shouldMoveTheCursorALineOnJAndKAndKeepItWithinTheDiff() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());

        shell.press(key('j'));
        assertThat(cursorText(shell)).contains("- old");
        shell.press(key('k'));
        shell.press(key('k'));
        assertThat(cursorText(shell)).contains("keep");
    }

    @Test
    void shouldStopTheCursorOnTheLastLineOfTheDiff() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());

        ShellFixture.pressed(shell, key('j'), 20);

        assertThat(cursorText(shell)).contains("+ second");
    }

    @Test
    void shouldJumpToTheFirstLineOfTheNextAndPreviousFileOnShiftJAndShiftK() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());

        shell.press(key('J'));
        assertThat(cursorText(shell)).contains("+ first");
        shell.press(key('J'));
        assertThat(cursorText(shell)).as("no file after the last").contains("+ first");
        shell.press(key('K'));
        assertThat(cursorText(shell)).contains("keep");
        shell.press(key('K'));
        assertThat(cursorText(shell)).as("no file before the first").contains("keep");
    }

    @Test
    void shouldMoveHalfAPageOnCtrlDAndCtrlU() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());
        Screen.of(shell);

        shell.press(ctrl('d'));
        assertThat(cursorText(shell)).as("half of the 9 rows the main view is high is 4 lines").contains("+ first");
        shell.press(ctrl('u'));
        assertThat(cursorText(shell)).contains("keep");
    }

    @Test
    void shouldHaveTheFilesSelectionFollowTheCursorsFile() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());

        shell.press(key('J'));

        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("A Docs.java"));
    }

    @Test
    void shouldMoveTheCursorToTheFirstLineOfAFileOnEnterInFiles() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());
        shell.press(key('h'));

        shell.press(key('j'));
        assertThat(cursorText(shell)).as("j in Files moves its selection, not the cursor").contains("keep");
        shell.press(enter());

        assertThat(cursorText(shell)).contains("+ first");
    }

    @Test
    void shouldMoveTheCursorToThePlacedThreadsLineOnEnterInComments() {
        Shell shell = shellReviewing(twoFileDiffView(), threadsOnTwoFiles());
        shell.press(key('h'));
        shell.press(alt('j'));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.COMMENTS);
        assertThat(cursorText(shell)).contains("+ new");
    }

    @Test
    void shouldDoNothingOnEnterInCommentsOnAnOutdatedThread() {
        Shell shell = shellReviewing(twoFileDiffView(), threadsOnTwoFiles());
        shell.press(key('h'));
        shell.press(alt('j'));
        shell.press(key('j'));
        shell.press(key('j'));

        shell.press(enter());

        assertThat(cursorText(shell)).contains("keep");
    }

    @Test
    void shouldShowResolvedThreadsDimOnRAndHideThemOnRAgain() {
        Shell shell = shellReviewing(twoFileDiffView(), threadsOnTwoFiles());
        shell.press(key('J'));
        assertThat(Screen.of(shell).mainViewRows()).noneSatisfy(row -> assertThat(row).contains("L1 alice"));

        shell.press(key('r'));
        shell.press(key('j'));
        Screen shown = Screen.of(shell);
        assertThat(shown.mainViewRows()).anySatisfy(row -> assertThat(row).contains("◆ L1 alice"));
        assertThat(shown.dimText()).anySatisfy(text -> assertThat(text).contains("◆ L1 alice"));
        shell.press(key('r'));

        assertThat(Screen.of(shell).mainViewRows()).noneSatisfy(row -> assertThat(row).contains("L1 alice"));
    }

    @Test
    void shouldNeverPlaceAnOutdatedThreadInlineEvenWithResolvedOnesShown() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf(outdatedThreadView(diffPath())));

        shell.press(key('r'));

        assertThat(Screen.of(shell).mainViewRows()).noneSatisfy(row -> assertThat(row).contains("◆"));
    }

    @Test
    void shouldMoveFocusBetweenFilesCommentsAndTheMainViewWithHLAndAltJK() {
        Shell shell = shellReviewing(twoFileDiffView(), loadingThreadsView());

        shell.press(key('h'));
        assertThat(shell.focus()).isEqualTo(Focus.FILES);
        shell.press(alt('j'));
        assertThat(shell.focus()).isEqualTo(Focus.COMMENTS);
        shell.press(alt('j'));
        assertThat(shell.focus()).as("the panels do not run on into the four a review replaced").isEqualTo(Focus.COMMENTS);
        shell.press(key('l'));
        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        shell.press(key('h'));
        assertThat(shell.focus()).isEqualTo(Focus.COMMENTS);
    }

    @Test
    void shouldListTheReviewsKeysInTheKeyBar() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), loadingThreadsView()));

        assertThat(screen.lines().getLast())
                .contains("c comment  v range  enter edit  x delete  S submit  j/k line");
    }

    @Test
    void shouldReadNothingMoreOnPInsideTheReview() {
        ReviewQuery reviews = reviewsAnswering(twoFileDiffView(), loadingThreadsView());
        Shell shell = shellOnATicketWithAnOpenPullRequest(reviews, Runnable::run);
        shell.press(key('p'));

        shell.press(key('p'));

        verify(reviews, times(1)).diffOf(any());
    }
}

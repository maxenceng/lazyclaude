package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.movedOpenPendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.commentOnView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.openPendingViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.replyViewOn;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftRange;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.outdated;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.unavailablePendingView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.pendingOnBothFiles;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellReviewing;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.threadsOnTwoFiles;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;

class ShellReviewPendingTest {

    @Test
    void shouldMarkAFileWithItsPendingCommentsBesideItsOpenThreadsAndNothingWhenItHasNone() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), pendingOnBothFiles()));

        assertThat(screen.boardPanelLines())
                .anySatisfy(row -> assertThat(row).startsWith("M Board.java").endsWith("◆1 ✎2"))
                .anySatisfy(row -> assertThat(row).startsWith("A Docs.java").endsWith("✎1").doesNotContain("◆"));
        assertThat(Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), noPendingReviewView())).text()).doesNotContain("✎");
    }

    @Test
    void shouldListPendingCommentsFirstInDiffOrderThenTheThreadsEachPendingOneMarkedInItsOwnColour() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), pendingOnBothFiles()));

        assertThat(screen.boardPanelLines())
                .containsSubsequence("✎ Board.java:3", "✎ Board.java:3", "✎ Docs.java:1", "◆ Board.java:3 alice", "  Docs.java:1 alice");
        assertThat(screen.foregroundTextOnLine(screen.lineOf("✎ Docs.java:1"), PendingText.COLOUR)).startsWith("✎");
    }

    @Test
    void shouldListAPendingCommentOnNoLineWithTheFileAndOutdated() {
        Screen screen = Screen.of(shellReviewing(
                twoFileDiffView(), threadsOnTwoFiles(), openPendingViewOf(replyViewOn(diffPath(), outdated()))));

        assertThat(screen.boardPanelLines()).anySatisfy(row -> assertThat(row).isEqualTo("✎ Board.java  outdated"));
    }

    @Test
    void shouldShowTheSummarysFirstLineAndThePendingCountInAReviewPanelUnderFilesAndComments() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), pendingOnBothFiles()));

        assertThat(screen.text()).contains("╭Review");
        assertThat(screen.lineOf("╭Review")).isGreaterThan(screen.lineOf("╭Comments"));
        assertThat(screen.boardPanelLines()).containsSubsequence("Looks close. Two nits.", "3 pending");
        assertThat(screen.text()).doesNotContain("The second is about naming.", "written on");
    }

    @Test
    void shouldWarnWithBothCommitsInTheWarningColourWhenTheHeadMovedPastTheCommitTheReviewWasWrittenOn() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), movedOpenPendingView()));

        assertThat(screen.boardPanelLines()).containsSubsequence("written on 9f3c2e1,", "head is now 4b1d0a7");
        assertThat(screen.foregroundText(Color.YELLOW)).anySatisfy(text -> assertThat(text).contains("written on 9f3c2e1,"));
    }

    @Test
    void shouldSayNoPendingReviewDimWhenThereIsNone() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), noPendingReviewView()));

        assertThat(screen.boardPanelLines()).contains("no pending review");
        assertThat(screen.dimText()).anySatisfy(text -> assertThat(text).contains("no pending review"));
    }

    @Test
    void shouldSayLoadingWhileThePendingReviewIsNotRead() {
        Shell shell = ShellReviewFixture.shellReviewingWhileTheReadsHang(twoFileDiffView(), threadsOnTwoFiles());

        assertThat(Screen.of(shell).boardPanelLines()).contains("loading…");
    }

    @Test
    void shouldSayWhyThePendingReviewIsUnknownDimWhenItCannotBeRead() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), threadsOnTwoFiles(), unavailablePendingView()));

        assertThat(screen.boardPanelText()).contains("pending unknown (gh: not");
        assertThat(screen.dimText()).anySatisfy(text -> assertThat(text).contains("pending unknown"));
    }

    @Test
    void shouldShowAPendingCommentUnderItsLineInItsOwnColourAsComingFromYou() {
        Screen screen = Screen.of(shellReviewing(twoFileDiffView(), readThreadsViewOf(), openPendingViewOf(commentOnView(diffPath(), rightOn(3)))));

        assertThat(screen.mainViewRows().stream().map(String::strip).toList())
                .containsSubsequence("3 + new", "✎ L3 you", "Does this need its own Element?");
        assertThat(screen.foregroundTextOnLine(screen.lineOf("✎ L3 you"), PendingText.COLOUR)).contains("✎ L3");
        assertThat(screen.text()).doesNotContain("◆ L3");
    }

    @Test
    void shouldShowAPendingReplyUnderTheLineOfItsThreadAfterThePlacedThread() {
        Screen screen = Screen.of(shellReviewing(
                twoFileDiffView(),
                readThreadsViewOf(openThreadView(diffPath(), rightOn(3))),
                openPendingViewOf(replyViewOn(diffPath(), rightOn(3)))));

        assertThat(screen.mainViewRows().stream().map(String::strip).toList()).containsSubsequence("◆ L3 alice", "bob  It assumes markdown.", "✎ reply you");
    }

    @Test
    void shouldShowAPendingRangeAsItsLines() {
        Screen screen = Screen.of(shellReviewing(
                twoFileDiffView(), readThreadsViewOf(), openPendingViewOf(commentOnView(diffPath(), leftRange(2, 3)))));

        assertThat(screen.mainViewRows()).anySatisfy(row -> assertThat(row).contains("✎ L2–3 you"));
    }
}

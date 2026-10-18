package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.resolvedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.threadWithALongAuthorView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.diffOfLines;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.drawn;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.paneOver;
import static dev.maxenceng.lazyclaude.shell.ReviewPaneFixture.paneOverTwoFiles;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;

class DiffElementTest {

    @Test
    void shouldDrawEachFileUnderARuleWithItsPathAndEachHunkUnderItsHeader() {
        Screen screen = drawn(paneOverTwoFiles(), 12);

        assertThat(screen.lines().getFirst()).startsWith("── src/Board.java ─────").endsWith("─");
        assertThat(screen.lines().get(1)).startsWith("@@ -2,4 +2,4 @@ class Board {");
        assertThat(screen.dimText()).contains("@@ -2,4 +2,4 @@ class Board {");
    }

    @Test
    void shouldDrawTheOldAndNewNumbersInTwoGuttersBeforeTheMarkerAndTheLine() {
        Screen screen = drawn(paneOverTwoFiles(), 12);

        assertThat(screen.lines().get(2)).startsWith("▌   2    2   keep");
        assertThat(screen.lines().get(3)).startsWith("    3      - old");
        assertThat(screen.lines().get(4)).startsWith("         3 + new");
    }

    @Test
    void shouldDrawAddedLinesGreenAndRemovedLinesRed() {
        Screen screen = drawn(paneOverTwoFiles(), 12);

        assertThat(screen.foregroundTextOnLine(4, Color.GREEN)).isEqualTo("+ new");
        assertThat(screen.foregroundTextOnLine(3, Color.RED)).isEqualTo("- old");
    }

    @Test
    void shouldReverseOnlyTheRowTheCursorIsOn() {
        ReviewPane pane = paneOverTwoFiles();
        pane.lineDown();

        Screen screen = drawn(pane, 12);

        assertThat(screen.reversedTextOnLine(3)).startsWith("▌").contains("- old");
        assertThat(screen.reversedTextOnLine(2)).isEmpty();
    }

    @Test
    void shouldDrawAThreadUnderItsLineWithItsRangeAndAuthorThenItsCommentsWithTheReplyIndentedUnderTheFirst() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(openThreadView(diffPath(), rightOn(3))));

        Screen screen = drawn(pane, 12);

        assertThat(screen.lines().subList(4, 8))
                .satisfiesExactly(
                        line -> assertThat(line).startsWith("         3 + new"),
                        line -> assertThat(line.stripTrailing()).isEqualTo("           ◆ L3 alice"),
                        line -> assertThat(line.stripTrailing()).isEqualTo("             Why not reuse it?"),
                        line -> assertThat(line.stripTrailing()).isEqualTo("               bob  It assumes markdown."));
        assertThat(screen.foregroundTextOnLine(5, Color.MAGENTA)).isEqualTo("◆ L3");
    }

    @Test
    void shouldDrawARangeFromItsStartLineToItsLine() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(openThreadView(diffPath(), rightRange(2, 3))));

        assertThat(drawn(pane, 12).lines().get(5)).contains("◆ L2–3 alice");
    }

    @Test
    void shouldWrapACommentToTheWidthLeftOfTheGutters() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(openThreadView(diffPath(), rightOn(3))));

        Screen screen = Screen.of(new DiffElement(pane), 30, 12);

        assertThat(screen.lines()).anySatisfy(line -> assertThat(line.stripTrailing()).isEqualTo("             Why not reuse it?").hasSizeLessThanOrEqualTo(30));
    }

    @Test
    void shouldKeepEveryThreadRowInsideTheAreaWhenAnAuthorLeavesNoRoomForTheReply() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(threadWithALongAuthorView(diffPath(), rightOn(3))));

        Screen screen = Screen.of(new DiffElement(pane), 40, 12, Rect.of(24, 12));

        assertThat(screen.lines())
                .as("a row drawn past the area's right edge overwrites the main panel's border")
                .allSatisfy(line -> assertThat(line.substring(24)).isBlank());
    }

    @Test
    void shouldDimAResolvedThreadWhenResolvedOnesAreShown() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(resolvedThreadView(diffPath(), rightOn(3))));
        pane.toggleResolved();

        Screen screen = drawn(pane, 12);

        assertThat(screen.dimText()).contains("◆ L3 alice", "Why not reuse it?");
    }

    @Test
    void shouldDrawNoThreadForAResolvedOneUntilResolvedOnesAreShown() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(resolvedThreadView(diffPath(), rightOn(3))));

        assertThat(drawn(pane, 12).text()).doesNotContain("◆ L3");
    }

    @Test
    void shouldDrawNoThreadWhenTheThreadsWereNotRead() {
        ReviewPane pane = paneOver(twoFileDiffView(), loadingThreadsView());

        assertThat(drawn(pane, 12).text()).doesNotContain("◆");
    }

    @Test
    void shouldCutAThreadOffAtTheBottomOfTheArea() {
        ReviewPane pane = paneOver(twoFileDiffView(), readThreadsViewOf(openThreadView(diffPath(), rightOn(3))));
        pane.lineDown();
        pane.lineDown();

        Screen screen = drawn(pane, 6);

        assertThat(screen.lines().get(4)).contains("+ new");
        assertThat(screen.lines().get(5)).contains("◆ L3 alice");
    }

    @Test
    void shouldScrollJustFarEnoughToKeepTheCursorsLineInView() {
        ReviewPane pane = paneOver(diffOfLines(100), loadingThreadsView());
        drawn(pane, 10);
        pane.halfPageDown();
        pane.halfPageDown();
        pane.lineDown();

        Screen screen = drawn(pane, 10);

        assertThat(screen.lines().get(9)).contains("line 12");
        assertThat(screen.reversedTextOnLine(9)).contains("line 12");
    }

    @Test
    void shouldScrollBackUpToTheFileAndHunkHeadersWhenTheCursorGoesAboveTheTop() {
        ReviewPane pane = paneOver(diffOfLines(100), loadingThreadsView());
        drawn(pane, 10);
        pane.halfPageDown();
        pane.halfPageDown();
        drawn(pane, 10);
        pane.halfPageUp();
        pane.halfPageUp();

        Screen screen = drawn(pane, 10);

        assertThat(screen.lines().getFirst()).startsWith("── src/Board.java");
        assertThat(screen.lines().get(2)).contains("line 1");
    }

    @Test
    void shouldDrawOnlyTheVisibleSliceOfADiffOfTwentyThousandLines() {
        ReviewPane pane = paneOver(diffOfLines(20_000), loadingThreadsView());
        pane.observeHeight(10_000);
        pane.halfPageDown();
        pane.halfPageDown();
        pane.halfPageDown();

        Screen screen = drawn(pane, 10);

        assertThat(screen.text()).contains("line 15001").doesNotContain("line 1 ", "line 14000 ");
        assertThat(screen.reversedTextOnLine(9)).contains("line 15001");
    }

    @Test
    void shouldKeepTheCursorsLineInViewInAnAreaOneRowHigh() {
        assertThat(drawn(paneOverTwoFiles(), 1).lines().getFirst()).startsWith("▌   2    2   keep");
    }
}

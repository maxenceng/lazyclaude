package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.addedFileView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.changedFileView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.openThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.outdatedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readDiffViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.renamedFileView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.resolvedThreadView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.unavailableThreadsView;
import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.added;
import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.context;
import static dev.maxenceng.lazyclaude.review.published.DiffLineFixture.removed;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.published.HunkHeader;

class DiffRowsTest {

    @Test
    void shouldListEachFileUnderItsHeaderAndEachHunkUnderItsOwn() {
        DiffRows rows = DiffRows.of(twoFileDiffView(), loadingThreadsView(), false);

        assertThat(rows.size()).isEqualTo(10);
        assertThat(rows.row(0)).isEqualTo(new FileHeaderRow(changedFileView()));
        assertThat(rows.row(1)).isEqualTo(new HunkHeaderRow(new HunkHeader("@@ -2,4 +2,4 @@ class Board {")));
        assertThat(rows.row(2)).isEqualTo(new LineRow(context("keep", 2, 2), 0));
        assertThat(rows.row(6)).isEqualTo(new FileHeaderRow(addedFileView()));
        assertThat(rows.row(8)).isEqualTo(new LineRow(added("first", 1), 1));
    }

    @Test
    void shouldCountOnlyDiffLinesAsPlacesTheCursorCanBe() {
        DiffRows rows = DiffRows.of(twoFileDiffView(), loadingThreadsView(), false);

        assertThat(rows.lineCount()).isEqualTo(6);
        assertThat(rows.rowOfLine(0)).isEqualTo(2);
        assertThat(rows.rowOfLine(3)).isEqualTo(5);
        assertThat(rows.rowOfLine(4)).isEqualTo(8);
    }

    @Test
    void shouldPlaceAThreadOnTheNewSideUnderTheAddedLineItNames() {
        ReviewThreadView thread = openThreadView(diffPath(), rightOn(3));

        DiffRows rows = DiffRows.of(twoFileDiffView(), readThreadsViewOf(thread), false);

        assertThat(rows.row(rows.rowOfLine(2) + 1)).isEqualTo(new ThreadRow(thread));
        assertThat(rows.row(rows.rowOfLine(1) + 1)).isEqualTo(new LineRow(added("new", 3), 0));
    }

    @Test
    void shouldPlaceAThreadOnTheOldSideUnderTheRemovedLineItNamesNotTheAddedOneWithTheSameNumber() {
        ReviewThreadView thread = openThreadView(diffPath(), leftOn(3));

        DiffRows rows = DiffRows.of(twoFileDiffView(), readThreadsViewOf(thread), false);

        assertThat(rows.row(rows.rowOfLine(1) + 1)).isEqualTo(new ThreadRow(thread));
        assertThat(rows.row(rows.rowOfLine(2) + 1)).isEqualTo(new LineRow(context("end", 4, 4), 0));
    }

    @Test
    void shouldPlaceAThreadOnAContextLineOnEitherSideItNames() {
        ReviewThreadView onTheNewSide = openThreadView(diffPath(), rightOn(2));
        ReviewThreadView onTheOldSide = openThreadView(diffPath(), leftOn(4));

        DiffRows rows = DiffRows.of(twoFileDiffView(), readThreadsViewOf(onTheNewSide, onTheOldSide), false);

        assertThat(rows.row(rows.rowOfLine(0) + 1)).isEqualTo(new ThreadRow(onTheNewSide));
        assertThat(rows.row(rows.rowOfLine(3) + 1)).isEqualTo(new ThreadRow(onTheOldSide));
    }

    @Test
    void shouldPlaceARangeUnderTheLastLineOfIt() {
        ReviewThreadView thread = openThreadView(diffPath(), rightRange(2, 3));

        DiffRows rows = DiffRows.of(twoFileDiffView(), readThreadsViewOf(thread), false);

        assertThat(rows.row(rows.rowOfLine(2) + 1)).isEqualTo(new ThreadRow(thread));
        assertThat(rows.row(rows.rowOfLine(0) + 1)).isEqualTo(new LineRow(removed("old", 3), 0));
    }

    @Test
    void shouldNotPlaceAThreadOnTheFileOfAnotherPathWithTheSameLine() {
        DiffRows rows = DiffRows.of(twoFileDiffView(), readThreadsViewOf(openThreadView(secondDiffPath(), rightOn(3))), false);

        assertThat(rows.size()).isEqualTo(10);
    }

    @Test
    void shouldHideAResolvedThreadUntilResolvedOnesAreShown() {
        ReviewThreadView resolved = resolvedThreadView(diffPath(), rightOn(3));

        assertThat(DiffRows.of(twoFileDiffView(), readThreadsViewOf(resolved), false).size()).isEqualTo(10);
        assertThat(DiffRows.of(twoFileDiffView(), readThreadsViewOf(resolved), true).size()).isEqualTo(11);
    }

    @Test
    void shouldNeverPlaceAnOutdatedThreadInline() {
        DiffRows rows = DiffRows.of(twoFileDiffView(), readThreadsViewOf(outdatedThreadView(diffPath())), true);

        assertThat(rows.size()).isEqualTo(10);
    }

    @Test
    void shouldPlaceNoThreadWhenTheThreadsWereNotRead() {
        assertThat(DiffRows.of(twoFileDiffView(), unavailableThreadsView(), true).size()).isEqualTo(10);
    }

    @Test
    void shouldHoldNoRowsWhenTheDiffWasNotRead() {
        DiffRows rows = DiffRows.of(loadingDiffView(), loadingThreadsView(), false);

        assertThat(rows.size()).isZero();
        assertThat(rows.lineCount()).isZero();
        assertThat(rows.files()).isEmpty();
    }

    @Test
    void shouldFindTheFirstLineOfEachFileAndNoneInAFileWithoutLines() {
        DiffRows rows = DiffRows.of(readDiffViewOf(changedFileView(), renamedFileView(), addedFileView()), loadingThreadsView(), false);

        assertThat(rows.firstLineOf(0)).hasValue(0);
        assertThat(rows.firstLineOf(1)).isEmpty();
        assertThat(rows.firstLineOf(2)).hasValue(4);
    }

    @Test
    void shouldSayWhichFileALineBelongsTo() {
        DiffRows rows = DiffRows.of(twoFileDiffView(), loadingThreadsView(), false);

        assertThat(rows.fileOfLine(3)).isZero();
        assertThat(rows.fileOfLine(4)).isEqualTo(1);
    }

    @Test
    void shouldFindTheFirstLineOfTheNextAndOfThePreviousFileSkippingFilesWithoutLines() {
        DiffRows rows = DiffRows.of(readDiffViewOf(changedFileView(), renamedFileView(), addedFileView()), loadingThreadsView(), false);

        assertThat(rows.firstLineOfNextFile(1)).hasValue(4);
        assertThat(rows.firstLineOfNextFile(5)).isEmpty();
        assertThat(rows.firstLineOfPreviousFile(5)).hasValue(0);
        assertThat(rows.firstLineOfPreviousFile(2)).isEmpty();
    }

    @Test
    void shouldFindTheLineAThreadSitsOn() {
        DiffRows rows = DiffRows.of(twoFileDiffView(), loadingThreadsView(), false);

        assertThat(rows.lineOf(openThreadView(diffPath(), rightOn(3)))).hasValue(2);
        assertThat(rows.lineOf(openThreadView(diffPath(), leftOn(3)))).hasValue(1);
        assertThat(rows.lineOf(openThreadView(diffPath(), rightRange(2, 3)))).hasValue(2);
    }

    @Test
    void shouldFindNoLineForAnOutdatedThreadOrOneOnALineTheDiffDoesNotHold() {
        DiffRows rows = DiffRows.of(twoFileDiffView(), loadingThreadsView(), false);

        assertThat(rows.lineOf(outdatedThreadView(diffPath()))).isEmpty();
        assertThat(rows.lineOf(openThreadView(diffPath(), rightOn(99)))).isEmpty();
    }
}

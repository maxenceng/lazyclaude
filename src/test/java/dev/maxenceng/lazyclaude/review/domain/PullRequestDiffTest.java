package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.addedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.fileWithOldNumbersFarAboveNewOnes;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.onLine;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.onNoLine;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.outdatedThreadIn;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.threadAt;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PullRequestDiffTest {

    @Test
    void shouldPutARightThreadOnTheLineOfItsNewNumberAndALeftOneOnTheLineOfItsOldNumber() {
        ReviewThread onTheNewSide = threadAt(diffPath(), rightOn(3));
        ReviewThread onTheOldSide = threadAt(diffPath(), leftOn(3));

        assertThat(pullRequestDiffOf(modifiedFile()).place(new ReadThreads(List.of(onTheNewSide, onTheOldSide))).threads())
                .as("new line 3 is the added line, old line 3 the removed one above it")
                .containsExactly(onLine(onTheOldSide, 1), onLine(onTheNewSide, 2));
    }

    @Test
    void shouldPutAThreadOnAContextLineFromEitherSide() {
        ReviewThread onTheNewSide = threadAt(diffPath(), rightOn(5));
        ReviewThread onTheOldSide = threadAt(diffPath(), leftOn(4));

        assertThat(pullRequestDiffOf(modifiedFile()).place(new ReadThreads(List.of(onTheNewSide, onTheOldSide))).threads())
                .containsExactlyInAnyOrder(onLine(onTheNewSide, 4), onLine(onTheOldSide, 4));
    }

    @Test
    void shouldPutARangeOnItsLastLine() {
        ReviewThread range = threadAt(diffPath(), rightRange(2, 3));

        assertThat(pullRequestDiffOf(modifiedFile()).place(new ReadThreads(List.of(range))).threads()).containsExactly(onLine(range, 2));
    }

    @Test
    void shouldCountLinesAcrossFilesInTheOrderTheDiffListsThem() {
        ReviewThread inSecond = threadAt(secondDiffPath(), rightOn(1));

        assertThat(pullRequestDiffOf(modifiedFile(), addedFile()).place(new ReadThreads(List.of(inSecond))).threads())
                .containsExactly(onLine(inSecond, 5));
    }

    @Test
    void shouldListThreadsInDiffOrderEvenWhereTheirLineNumbersSayOtherwise() {
        ReviewThread onNewLineFifty = threadAt(diffPath(), rightOn(50));
        ReviewThread onOldLineHundred = threadAt(diffPath(), leftOn(100));

        assertThat(pullRequestDiffOf(fileWithOldNumbersFarAboveNewOnes())
                        .place(new ReadThreads(List.of(onNewLineFifty, onOldLineHundred)))
                        .threads())
                .as("old line 100 is removed above where new line 50 is added, so its thread shows first")
                .containsExactly(onLine(onOldLineHundred, 0), onLine(onNewLineFifty, 1));
    }

    @Test
    void shouldPutOnNoLineAndLastAnOutdatedThreadOrOneOnALineTheDiffDoesNotHold() {
        ReviewThread outdated = outdatedThreadIn(diffPath());
        ReviewThread offTheDiff = threadAt(diffPath(), rightOn(99));
        ReviewThread placed = threadAt(diffPath(), rightOn(3));

        assertThat(pullRequestDiffOf(modifiedFile()).place(new ReadThreads(List.of(outdated, offTheDiff, placed))).threads())
                .containsExactly(onLine(placed, 2), onNoLine(offTheDiff), onNoLine(outdated));
    }

    @Test
    void shouldNotPutAThreadOnTheLineOfAnotherFileWithTheSameNumber() {
        ReviewThread inSecond = threadAt(secondDiffPath(), rightOn(3));

        assertThat(pullRequestDiffOf(modifiedFile()).place(new ReadThreads(List.of(inSecond))).threads())
                .containsExactly(onNoLine(inSecond));
    }
}

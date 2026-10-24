package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.addedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.fileWithTwoHunks;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiff;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftRange;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;

class PullRequestDiffCommentTest {

    @Test
    void shouldCommentOnAnAddedLineOnTheNewSideAtItsNewNumber() {
        assertThat(pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(2), new DiffLineIndex(2)))
                .isEqualTo(onDiff(diffPath(), rightOn(3)));
    }

    @Test
    void shouldCommentOnARemovedLineOnTheOldSideAtItsOldNumber() {
        assertThat(pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(1), new DiffLineIndex(1)))
                .isEqualTo(onDiff(diffPath(), leftOn(3)));
    }

    @Test
    void shouldCommentOnAContextLineOnTheNewSide() {
        assertThat(pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(0), new DiffLineIndex(0)))
                .isEqualTo(onDiff(diffPath(), rightOn(2)));
    }

    @Test
    void shouldCarryTheFirstLineOfARangeAsItsStart() {
        assertThat(pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(2), new DiffLineIndex(3)))
                .isEqualTo(onDiff(diffPath(), rightRange(3, 4)));
    }

    @Test
    void shouldPutARangeOfUnchangedAndRemovedLinesOnTheOldSideWhicheverComesFirst() {
        assertThat(pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(0), new DiffLineIndex(1)))
                .isEqualTo(onDiff(diffPath(), leftRange(2, 3)));
        assertThat(pullRequestDiffOf(fileWithTwoHunks()).commentOn(new DiffLineIndex(2), new DiffLineIndex(3)))
                .as("a removed line has no new number, so a range starting on one is on the old side even when it ends on an unchanged line")
                .isEqualTo(onDiff(diffPath(), leftRange(40, 41)));
    }

    @Test
    void shouldRefuseARangeHoldingBothARemovedAndAnAddedLine() {
        assertThatThrownBy(() -> pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(1), new DiffLineIndex(3)))
                .isInstanceOf(RangeAcrossSidesException.class);
    }

    @Test
    void shouldTakeARangeSelectedUpwardsAsTheSameRange() {
        assertThat(pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(3), new DiffLineIndex(2)))
                .isEqualTo(onDiff(diffPath(), rightRange(3, 4)));
    }

    @Test
    void shouldAcceptARangeWhoseLastLineIsTheLastLineOfItsHunk() {
        assertThat(pullRequestDiffOf(fileWithTwoHunks()).commentOn(new DiffLineIndex(0), new DiffLineIndex(1)))
                .isEqualTo(onDiff(diffPath(), rightRange(2, 3)));
    }

    @Test
    void shouldRefuseARangeThatRunsOneLinePastItsHunk() {
        assertThatThrownBy(() -> pullRequestDiffOf(fileWithTwoHunks()).commentOn(new DiffLineIndex(0), new DiffLineIndex(2)))
                .isInstanceOf(RangeAcrossHunksException.class);
    }

    @Test
    void shouldRefuseARangeAcrossTwoFiles() {
        assertThatThrownBy(() -> pullRequestDiffOf(modifiedFile(), addedFile()).commentOn(new DiffLineIndex(4), new DiffLineIndex(5)))
                .isInstanceOf(RangeAcrossHunksException.class);
    }

    @Test
    void shouldCountLinesAcrossFilesWhenCommentingOnTheSecond() {
        assertThat(pullRequestDiffOf(modifiedFile(), addedFile()).commentOn(new DiffLineIndex(5), new DiffLineIndex(5)))
                .isEqualTo(onDiff(secondDiffPath(), rightOn(1)));
    }

    @Test
    void shouldRefuseALineTheDiffDoesNotHold() {
        assertThatThrownBy(() -> pullRequestDiffOf(modifiedFile()).commentOn(new DiffLineIndex(5), new DiffLineIndex(5)))
                .isInstanceOf(DiffLineMissingException.class);
    }
}

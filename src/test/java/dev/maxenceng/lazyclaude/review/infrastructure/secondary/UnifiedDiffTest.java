package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.addedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.deletedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.fileFromTheFirstLine;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.fileWithBlankContext;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.fileWithTwoHunks;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.fileWithoutHunks;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.renamedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.renamedFileWithoutChanges;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.addedFileDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.binaryFileDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.blankContextDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.deletedFileDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.fileLineNamingABlankPath;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.fileLineTooShortToNameAPath;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.firstLineDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.hunkHeaderPastAnyLineNumber;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.hunkHeaderWithoutNumbers;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.modifiedFileDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.renamedFileDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.renamedFileWithoutChangesDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.twoFilesDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.twoHunksDiff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

class UnifiedDiffTest {

    @Test
    void shouldReadAModifiedFileWithItsHunkAndNumberEachLineOnTheSidesItBelongsTo() {
        assertThat(UnifiedDiff.parse(modifiedFileDiff())).isEqualTo(pullRequestDiffOf(modifiedFile()));
    }

    @Test
    void shouldReadAFileWithNewFileModeAsAddedAndNumberItsLinesOnTheNewSideOnly() {
        assertThat(UnifiedDiff.parse(addedFileDiff())).isEqualTo(pullRequestDiffOf(addedFile()));
    }

    @Test
    void shouldReadAFileWithDeletedFileModeAsDeletedAndNumberItsLinesOnTheOldSideOnly() {
        assertThat(UnifiedDiff.parse(deletedFileDiff())).isEqualTo(pullRequestDiffOf(deletedFile()));
    }

    @Test
    void shouldReadARenamedFileUnderItsNewPathCarryingTheOldOne() {
        assertThat(UnifiedDiff.parse(renamedFileDiff())).isEqualTo(pullRequestDiffOf(renamedFile()));
    }

    @Test
    void shouldReadARenamedFileWithoutChangesAsRenamedWithNoHunk() {
        assertThat(UnifiedDiff.parse(renamedFileWithoutChangesDiff()))

                .isEqualTo(pullRequestDiffOf(renamedFileWithoutChanges()));
    }

    @Test
    void shouldReadABinaryFileAsModifiedWithNoHunk() {
        assertThat(UnifiedDiff.parse(binaryFileDiff())).isEqualTo(pullRequestDiffOf(fileWithoutHunks()));
    }

    @Test
    void shouldReadAHunkAtLineOneWhoseCountsAreLeftOutAndSkipTheNoNewlineMarkers() {
        assertThat(UnifiedDiff.parse(firstLineDiff())).isEqualTo(pullRequestDiffOf(fileFromTheFirstLine()));
    }

    @Test
    void shouldNumberEachHunkFromItsOwnHeader() {
        assertThat(UnifiedDiff.parse(twoHunksDiff())).isEqualTo(pullRequestDiffOf(fileWithTwoHunks()));
    }

    @Test
    void shouldReadFilesInTheOrderTheDiffListsThem() {
        assertThat(UnifiedDiff.parse(twoFilesDiff()))

                .isEqualTo(pullRequestDiffOf(modifiedFile(), addedFile()));
    }

    @Test
    void shouldReadAnEmptyContextLineAndLinesThatLookLikeFileHeadersAsLines() {
        assertThat(UnifiedDiff.parse(blankContextDiff())).isEqualTo(pullRequestDiffOf(fileWithBlankContext()));
    }

    @Test
    void shouldReadNoFilesFromAnEmptyDiff() {
        assertThat(UnifiedDiff.parse("").files()).isEmpty();
    }

    @Test
    void shouldRefuseAHunkWhoseHeaderCarriesNoLineNumbers() {
        assertThatThrownBy(() -> UnifiedDiff.parse(hunkHeaderWithoutNumbers()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr diff does not parse");
    }

    @Test
    void shouldRefuseAFileLineTooShortToNameAPath() {
        assertThatThrownBy(() -> UnifiedDiff.parse(fileLineTooShortToNameAPath()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr diff does not parse");
    }

    @Test
    void shouldRefuseAFileLineNamingABlankPath() {
        assertThatThrownBy(() -> UnifiedDiff.parse(fileLineNamingABlankPath()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr diff does not parse");
    }

    @Test
    void shouldRefuseAHunkHeaderPastAnyLineNumber() {
        assertThatThrownBy(() -> UnifiedDiff.parse(hunkHeaderPastAnyLineNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr diff does not parse");
    }
}

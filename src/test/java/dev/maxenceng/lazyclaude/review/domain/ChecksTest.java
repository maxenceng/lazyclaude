package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.checksOf;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.failedAnalyse;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.failedBuild;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.passedLint;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.pendingContract;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

class ChecksTest {

    @Test
    void shouldListAFailedCheckBeforeAPendingOneSoTheCiResultIsFailed() {
        assertThat(checksOf(pendingContract(), failedBuild(), passedLint()).listed().getFirst().outcome())
                .as("failed wins over pending")
                .isEqualTo(CheckOutcome.FAILED);
    }

    @Test
    void shouldListAPendingCheckBeforeAPassedOneSoTheCiResultIsPending() {
        assertThat(checksOf(passedLint(), pendingContract()).listed().getFirst().outcome())
                .as("pending wins over passed")
                .isEqualTo(CheckOutcome.PENDING);
    }

    @Test
    void shouldListFailedThenPendingThenPassedAndByNameWithinAGroup() {
        assertThat(checksOf(passedLint(), failedBuild(), pendingContract(), failedAnalyse()).listed())
                .containsExactly(failedAnalyse(), failedBuild(), pendingContract(), passedLint());
    }
}

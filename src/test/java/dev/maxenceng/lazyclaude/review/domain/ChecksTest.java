package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.checksOf;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.failedAnalyse;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.failedBuild;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.noChecks;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.passedLint;
import static dev.maxenceng.lazyclaude.review.domain.CheckFixture.pendingContract;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.published.Failed;
import dev.maxenceng.lazyclaude.review.published.NoCiResult;
import dev.maxenceng.lazyclaude.review.published.Passed;
import dev.maxenceng.lazyclaude.review.published.Pending;

class ChecksTest {

    @Test
    void shouldFailWhenOneCheckFailedAndAnotherIsStillPending() {
        assertThat(checksOf(pendingContract(), failedBuild(), passedLint()).result())
                .as("failed wins over pending")
                .isEqualTo(new Failed());
    }

    @Test
    void shouldBePendingWhenNoneFailedAndOneHasNotFinished() {
        assertThat(checksOf(passedLint(), pendingContract()).result())
                .as("pending wins over passed")
                .isEqualTo(new Pending());
    }

    @Test
    void shouldPassWhenEveryCheckPassed() {
        assertThat(checksOf(passedLint()).result()).isEqualTo(new Passed());
    }

    @Test
    void shouldHaveNoCiResultWithoutChecks() {
        assertThat(noChecks().result())
                .as("passed would claim a green run that never happened")
                .isEqualTo(new NoCiResult());
    }

    @Test
    void shouldListFailedThenPendingThenPassedAndByNameWithinAGroup() {
        assertThat(checksOf(passedLint(), failedBuild(), pendingContract(), failedAnalyse()).listed())
                .containsExactly(failedAnalyse(), failedBuild(), pendingContract(), passedLint());
    }
}

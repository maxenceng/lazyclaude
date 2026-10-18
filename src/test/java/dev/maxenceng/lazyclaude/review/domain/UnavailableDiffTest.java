package dev.maxenceng.lazyclaude.review.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class UnavailableDiffTest {

    @Test
    void shouldRefuseToBuildWithoutSayingWhetherTheDiffIsTooLarge() {
        assertThatThrownBy(PullRequestDiffManagerFixture::unavailableDiffSayingNothingOfItsSize)
                .as("a default of false would let a caller who forgot the flag say GitHub produced the diff")
                .isInstanceOf(MissingMandatoryValueException.class);
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class UnavailableDiffViewTest {

    @Test
    void shouldRefuseToBuildWithoutSayingWhetherTheDiffIsTooLarge() {
        assertThatThrownBy(PullRequestDiffViewFixture::unavailableDiffViewSayingNothingOfItsSize)
                .as("the shell builds this view itself when a read fails, and a forgotten flag would show PR unknown for a diff too large")
                .isInstanceOf(MissingMandatoryValueException.class);
    }
}

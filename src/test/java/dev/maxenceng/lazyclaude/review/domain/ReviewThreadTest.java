package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.outdatedThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.resolvedThread;
import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.reviewThread;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReviewThreadTest {

    @Test
    void shouldBeOpenWhenItIsPlacedAndNotResolved() {
        assertThat(reviewThread().open()).isTrue();
    }

    @Test
    void shouldNotBeOpenOnceResolved() {
        assertThat(resolvedThread().open()).isFalse();
    }

    @Test
    void shouldNotBeOpenWhenOutdatedEvenIfNeverResolved() {
        assertThat(outdatedThread().open()).isFalse();
    }
}

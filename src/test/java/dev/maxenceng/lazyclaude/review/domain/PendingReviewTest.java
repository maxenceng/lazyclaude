package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReview;
import static dev.maxenceng.lazyclaude.review.published.CommitShaFixture.commitSha;
import static dev.maxenceng.lazyclaude.review.published.CommitShaFixture.movedCommitSha;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.published.HeldHead;
import dev.maxenceng.lazyclaude.review.published.MovedHead;

class PendingReviewTest {

    @Test
    void shouldHoldTheHeadWhileItIsTheCommitTheReviewWasWrittenOn() {
        assertThat(pendingReview().headMove(commitSha())).isEqualTo(new HeldHead());
    }

    @Test
    void shouldTellTheHeadMovedWhenItIsAnotherCommit() {
        assertThat(pendingReview().headMove(movedCommitSha()))
                .isEqualTo(MovedHead.builder().writtenOn(commitSha()).head(movedCommitSha()).build());
    }
}

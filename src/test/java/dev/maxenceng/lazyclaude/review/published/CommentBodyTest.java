package dev.maxenceng.lazyclaude.review.published;

import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.blankCommentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommentBodyTest {

    @Test
    void shouldSayNothingWhenItIsOnlyWhiteSpace() {
        assertThat(blankCommentBody().isBlank()).isTrue();
    }

    @Test
    void shouldSaySomethingWhenItHasText() {
        assertThat(commentBody().isBlank()).isFalse();
    }
}

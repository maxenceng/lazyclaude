package dev.maxenceng.lazyclaude.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.NegativeValueException;

class CommentCountTest {

    @Test
    void shouldCountTheConversationAndTheThreadsLessYourPendingComments() {
        assertThat(CommentCount.visible(2, 5, 3))
                .as("your pending comments are in the threads' count but nobody else sees them")
                .isEqualTo(new CommentCount(4));
    }

    @Test
    void shouldNeverCountBelowNoneWhenMorePendingCommentsThanThreadCommentsWereRead() {
        assertThat(CommentCount.visible(0, 1, 3))
                .as("only the first hundred threads are read, while every pending comment is")
                .isEqualTo(new CommentCount(0));
    }

    @Test
    void shouldRefuseANegativeCount() {
        assertThatThrownBy(() -> new CommentCount(-1)).isInstanceOf(NegativeValueException.class);
    }
}

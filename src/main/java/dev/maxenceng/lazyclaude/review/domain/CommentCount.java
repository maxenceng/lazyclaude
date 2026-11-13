package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * How many comments other people can see on a pull request: its conversation comments plus every comment of its review threads, the
 * replies included. Your own pending review's comments are left out, since nobody else sees them, and a review's summary is not a comment.
 */
public record CommentCount(int value) {

    public CommentCount {
        Assert.notNegative("comment count", value);
    }

    /**
     * The comments others see, from what GitHub counts: the conversation's, the review threads', and yours still pending among those
     * threads. Fewer threads may have been read than your pending comments sit in, so the count never goes below none.
     */
    public static CommentCount visible(int conversation, int inThreads, int yoursPending) {
        return new CommentCount(Math.max(0, conversation + inThreads - yoursPending));
    }
}

package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;

/** A comment in the pending review: what it says, and what it hangs on. */
public record PendingComment(PendingCommentId id, CommentBody body, PendingAnchor anchor) {

    public PendingComment {
        Assert.notNull("id", id);
        Assert.notNull("body", body);
        Assert.notNull("anchor", anchor);
    }

    public static PendingCommentBuilder builder() {
        return new PendingCommentBuilder();
    }

    public static final class PendingCommentBuilder {
        private PendingCommentId id;
        private CommentBody body;
        private PendingAnchor anchor;

        private PendingCommentBuilder() {}

        public PendingCommentBuilder id(PendingCommentId id) {
            this.id = id;
            return this;
        }

        public PendingCommentBuilder body(CommentBody body) {
            this.body = body;
            return this;
        }

        public PendingCommentBuilder anchor(PendingAnchor anchor) {
            this.anchor = anchor;
            return this;
        }

        public PendingComment build() {
            return new PendingComment(id, body, anchor);
        }
    }
}

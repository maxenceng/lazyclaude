package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.PendingComment;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;

/** A comment of the pending review: its id, what it says, and what it hangs on. */
public record PendingCommentView(PendingCommentId id, CommentBody body, PendingAnchorView anchor) {

    static PendingCommentView from(PendingComment comment) {
        return builder().id(comment.id()).body(comment.body()).anchor(PendingAnchorView.from(comment.anchor())).build();
    }

    PendingComment toDomain() {
        return PendingComment.builder().id(id).body(body).anchor(anchor.toDomain()).build();
    }

    public static PendingCommentViewBuilder builder() {
        return new PendingCommentViewBuilder();
    }

    public static final class PendingCommentViewBuilder {
        private PendingCommentId id;
        private CommentBody body;
        private PendingAnchorView anchor;

        private PendingCommentViewBuilder() {}

        public PendingCommentViewBuilder id(PendingCommentId id) {
            this.id = id;
            return this;
        }

        public PendingCommentViewBuilder body(CommentBody body) {
            this.body = body;
            return this;
        }

        public PendingCommentViewBuilder anchor(PendingAnchorView anchor) {
            this.anchor = anchor;
            return this;
        }

        public PendingCommentView build() {
            return new PendingCommentView(id, body, anchor);
        }
    }
}

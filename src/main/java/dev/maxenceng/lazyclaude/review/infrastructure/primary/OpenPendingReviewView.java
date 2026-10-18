package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PendingReview;
import dev.maxenceng.lazyclaude.review.published.CommitSha;
import dev.maxenceng.lazyclaude.review.published.HeadMove;
import dev.maxenceng.lazyclaude.review.published.Summary;

/** Your pending review of the pull request: its summary, its comments in the order GitHub lists them, and whether the head moved since it was written. */
public record OpenPendingReviewView(Summary summary, List<PendingCommentView> comments, HeadMove headMove)
        implements PendingReviewView {

    static OpenPendingReviewView from(PendingReview review, CommitSha head) {
        return builder()
                .summary(review.summary())
                .comments(review.comments().stream().map(PendingCommentView::from).toList())
                .headMove(review.headMove(head))
                .build();
    }

    public static OpenPendingReviewViewBuilder builder() {
        return new OpenPendingReviewViewBuilder();
    }

    public static final class OpenPendingReviewViewBuilder {
        private Summary summary;
        private List<PendingCommentView> comments;
        private HeadMove headMove;

        private OpenPendingReviewViewBuilder() {}

        public OpenPendingReviewViewBuilder summary(Summary summary) {
            this.summary = summary;
            return this;
        }

        public OpenPendingReviewViewBuilder comments(List<PendingCommentView> comments) {
            this.comments = comments;
            return this;
        }

        public OpenPendingReviewViewBuilder headMove(HeadMove headMove) {
            this.headMove = headMove;
            return this;
        }

        public OpenPendingReviewView build() {
            return new OpenPendingReviewView(summary, comments, headMove);
        }
    }
}

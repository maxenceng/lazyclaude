package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.CommitSha;
import dev.maxenceng.lazyclaude.review.published.HeadMove;
import dev.maxenceng.lazyclaude.review.published.HeldHead;
import dev.maxenceng.lazyclaude.review.published.MovedHead;
import dev.maxenceng.lazyclaude.review.published.Summary;

/** Your one unsubmitted review of a pull request, kept on GitHub: the commit it was written on, its summary and its pending comments. */
public record PendingReview(PendingReviewId id, CommitSha writtenOn, Summary summary, List<PendingComment> comments)
        implements ReviewInProgress {

    public PendingReview {
        Assert.notNull("id", id);
        Assert.notNull("written on", writtenOn);
        Assert.notNull("summary", summary);
        Assert.notNull("comments", comments);
    }

    /** Whether the pull request's head, {@code head}, is still the commit this review was written on. */
    public HeadMove headMove(CommitSha head) {
        return writtenOn.equals(head) ? new HeldHead() : MovedHead.builder().writtenOn(writtenOn).head(head).build();
    }

    public static PendingReviewBuilder builder() {
        return new PendingReviewBuilder();
    }

    public static final class PendingReviewBuilder {
        private PendingReviewId id;
        private CommitSha writtenOn;
        private Summary summary;
        private List<PendingComment> comments;

        private PendingReviewBuilder() {}

        public PendingReviewBuilder id(PendingReviewId id) {
            this.id = id;
            return this;
        }

        public PendingReviewBuilder writtenOn(CommitSha writtenOn) {
            this.writtenOn = writtenOn;
            return this;
        }

        public PendingReviewBuilder summary(Summary summary) {
            this.summary = summary;
            return this;
        }

        public PendingReviewBuilder comments(List<PendingComment> comments) {
            this.comments = comments;
            return this;
        }

        public PendingReview build() {
            return new PendingReview(id, writtenOn, summary, comments);
        }
    }
}

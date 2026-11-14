package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** Your pending review of a pull request, of which only how many comments it holds is read. */
record PendingReviewCountRecord(CommentCountRecord comments) {

    private static final String COUNT = "pending review comment count";

    MissingFields missingFields() {
        return comments == null ? new MissingFields().absent(COUNT, comments) : comments.missingFields(COUNT);
    }

    /** Only once {@link #missingFields()} names none. */
    int total() {
        return comments.total();
    }
}

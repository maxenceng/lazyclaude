package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** One review thread of a pull request, of which only how many comments it holds is read. */
record ThreadCommentCountRecord(CommentCountRecord comments) {

    private static final String COUNT = "review thread comment count";

    MissingFields missingFields() {
        return comments == null ? new MissingFields().absent(COUNT, comments) : comments.missingFields(COUNT);
    }

    /** Only once {@link #missingFields()} names none. */
    int total() {
        return comments.total();
    }
}

package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** A pending review as the pending review query writes it. */
record PendingReviewRecord(String id, String body, PendingReviewCommitRecord commit) {

    private static final String ID = "pending review id";
    private static final String BODY = "pending review body";
    private static final String COMMIT = "pending review commit";

    /** The fields a pending review cannot be read without that the JSON lacks. */
    MissingFields missingFields() {
        return new MissingFields()
                .blank(ID, id)
                .absent(BODY, body)
                .blank(COMMIT, Optional.ofNullable(commit).map(PendingReviewCommitRecord::oid).orElse(null));
    }
}

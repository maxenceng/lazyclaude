package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** A connection's {@code totalCount} as the GraphQL answer writes it, null where the JSON lacks it. */
record CommentCountRecord(Integer totalCount) {

    MissingFields missingFields(String field) {
        return new MissingFields().absent(field, totalCount);
    }

    /** Only once {@link #missingFields(String)} names none. */
    int total() {
        return totalCount;
    }
}

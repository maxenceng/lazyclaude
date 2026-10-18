package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

/** One page of the answer to the threads query, as {@code gh api graphql} writes it. */
record ThreadPageRecord(ThreadPageDataRecord data) {}

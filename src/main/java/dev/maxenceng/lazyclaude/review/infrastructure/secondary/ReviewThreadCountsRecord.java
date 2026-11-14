package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

/** The first review threads of a pull request, null where the JSON lacks them. */
record ReviewThreadCountsRecord(List<ThreadCommentCountRecord> nodes) {}

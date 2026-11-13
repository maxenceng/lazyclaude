package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

/** The issues a pull request closes, null where the JSON lacks them. */
record ClosingIssuesRecord(List<ClosingIssueRecord> nodes) {}

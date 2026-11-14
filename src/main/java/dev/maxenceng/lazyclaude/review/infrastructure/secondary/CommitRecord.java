package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

/** A commit's status rollup, null for a commit nothing checked. */
record CommitRecord(RollupRecord statusCheckRollup) {

    List<CheckRecord> checks() {
        return Optional.ofNullable(statusCheckRollup).map(RollupRecord::checks).orElseGet(List::of);
    }
}

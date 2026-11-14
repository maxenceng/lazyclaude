package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

/** A commit of a pull request as the GraphQL answer wraps it. */
record CommitNodeRecord(CommitRecord commit) {

    List<CheckRecord> checks() {
        return Optional.ofNullable(commit).map(CommitRecord::checks).orElseGet(List::of);
    }
}

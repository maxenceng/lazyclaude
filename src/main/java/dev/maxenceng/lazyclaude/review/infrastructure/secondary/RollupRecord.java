package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

/** The checks of a status rollup: its first 100 check runs and commit statuses. */
record RollupRecord(RollupContextsRecord contexts) {

    List<CheckRecord> checks() {
        return Optional.ofNullable(contexts).map(RollupContextsRecord::nodes).orElseGet(List::of);
    }
}

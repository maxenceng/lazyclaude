package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

/** The last commit of a pull request, null where the JSON lacks it; its checks are those of its status rollup, none when it has no rollup. */
record CommitsRecord(List<CommitNodeRecord> nodes) {

    List<CheckRecord> checks() {
        return Optional.ofNullable(nodes).stream()
                .flatMap(List::stream)
                .map(CommitNodeRecord::checks)
                .flatMap(List::stream)
                .toList();
    }
}

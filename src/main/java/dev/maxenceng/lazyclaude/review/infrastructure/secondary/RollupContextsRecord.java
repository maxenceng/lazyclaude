package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

/** The connection of checks of a status rollup, null where the JSON lacks it. */
record RollupContextsRecord(List<CheckRecord> nodes) {}

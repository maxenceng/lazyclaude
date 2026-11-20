package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

/** One label of an issue in the {@code labels} connection {@code gh api graphql} writes; the fields beyond its name are not read. */
record LabelRecord(String name) {}

package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

/** One label of an issue as {@code gh issue list --json labels} writes it; the fields beyond its name are not read. */
record LabelRecord(String name) {}

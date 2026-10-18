package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.DiffFileView;

/** A rule with the path of the file whose hunks follow. */
record FileHeaderRow(DiffFileView file) implements DiffRow {}

package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.published.HunkHeader;

/** The {@code @@} line that opens a hunk. */
record HunkHeaderRow(HunkHeader header) implements DiffRow {}

package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.published.DiffLine;

/** A line of a hunk, the only row the cursor lands on, and the index of the file it is in. */
record LineRow(DiffLine line, int file) implements DiffRow {}

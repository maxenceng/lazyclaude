package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.published.DiffLineNumber;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;

/** Where a thread hangs: a line number on one side of one file. */
record ThreadAnchor(DiffPath path, ThreadSide side, DiffLineNumber number) {}

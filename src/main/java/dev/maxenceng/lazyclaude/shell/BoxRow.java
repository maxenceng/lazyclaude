package dev.maxenceng.lazyclaude.shell;

/** The comment box drawn under the line it writes on, as tall as its wrapped text. */
record BoxRow(CommentBox box) implements DiffRow {}

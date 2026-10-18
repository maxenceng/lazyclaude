package dev.maxenceng.lazyclaude.review.published;

/** Where a thread sits in the diff: on a line, or nowhere since its line is gone. */
public sealed interface ThreadPosition permits PlacedThread, OutdatedThread {}

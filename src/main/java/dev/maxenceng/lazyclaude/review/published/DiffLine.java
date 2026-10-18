package dev.maxenceng.lazyclaude.review.published;

/** A line of a hunk: added, removed or unchanged context. The only rows a cursor or a comment lands on. */
public sealed interface DiffLine permits AddedLine, RemovedLine, ContextLine {}

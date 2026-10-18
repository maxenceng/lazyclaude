package dev.maxenceng.lazyclaude.review.published;

/** The diff line a thread shows under, or none: an outdated thread, or one whose line the diff does not hold or has not been read. */
public sealed interface ThreadLine permits OnDiffLine, NoDiffLine {}

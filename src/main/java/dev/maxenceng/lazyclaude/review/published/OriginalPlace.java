package dev.maxenceng.lazyclaude.review.published;

/** Where an outdated thread was when it was written: on a line or a range of one side, or nowhere GitHub still names. */
public sealed interface OriginalPlace permits OriginallyPlaced, NoOriginalPlace {}

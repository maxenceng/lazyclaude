package dev.maxenceng.lazyclaude.review.published;

/** The checks of a pull request collapsed into one answer; a pull request with no checks has none. */
public sealed interface CiResult permits Passed, Failed, Pending, NoCiResult {}

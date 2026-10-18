package dev.maxenceng.lazyclaude.review.domain;

/** What a read of a pull request's diff came to: not answered yet, the diff, or the reason it could not be read. */
public sealed interface DiffReading permits LoadingDiff, ReadDiff, UnavailableDiff {}

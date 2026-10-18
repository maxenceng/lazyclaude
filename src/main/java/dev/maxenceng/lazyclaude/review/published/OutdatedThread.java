package dev.maxenceng.lazyclaude.review.published;

/** A thread whose line the pull request no longer has, so it is placed on no line. */
public record OutdatedThread() implements ThreadPosition {}

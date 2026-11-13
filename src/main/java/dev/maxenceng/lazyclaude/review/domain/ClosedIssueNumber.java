package dev.maxenceng.lazyclaude.review.domain;

/** The number GitHub gave an issue that a pull request closes when it merges; the ticket it is, to whoever knows the board. */
public record ClosedIssueNumber(int value) {}

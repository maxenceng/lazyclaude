package dev.maxenceng.lazyclaude.review.domain;

/** The issue a pull request closes when it merges, or that it closes none in this repository. */
public sealed interface IssueClosing permits ClosesIssue, ClosesNoIssue {}

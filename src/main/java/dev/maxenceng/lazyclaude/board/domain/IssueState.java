package dev.maxenceng.lazyclaude.board.domain;

/** Whether a GitHub issue is open, or closed and why. */
public sealed interface IssueState permits OpenIssue, ClosedIssue {}

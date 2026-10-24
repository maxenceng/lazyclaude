package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** Where a new comment on the diff would hang: on the lines asked for, or nowhere since GitHub would refuse them. */
public sealed interface CommentAnchorView permits OnDiffAnchorView, RefusedAnchorView {}

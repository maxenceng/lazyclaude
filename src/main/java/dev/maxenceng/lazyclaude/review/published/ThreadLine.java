package dev.maxenceng.lazyclaude.review.published;

/** Where a thread shows in the diff: under a diff line, under the header of its file when the diff holds the file but not the line, or after the diff. */
public sealed interface ThreadLine permits OnDiffLine, AtFileHead, OutsideDiff {}

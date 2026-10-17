package dev.maxenceng.lazyclaude.review.published;

/** The commit a branch's merged pull request was merged at, or nothing when no merged pull request is known for the branch — never a stand-in. */
public sealed interface MergedHead permits HeadCommit, NoMergedHead {}

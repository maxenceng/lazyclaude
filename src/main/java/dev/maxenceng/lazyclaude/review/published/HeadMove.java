package dev.maxenceng.lazyclaude.review.published;

/** Whether the pull request's head is still the commit the pending review was written on. */
public sealed interface HeadMove permits MovedHead, HeldHead {}

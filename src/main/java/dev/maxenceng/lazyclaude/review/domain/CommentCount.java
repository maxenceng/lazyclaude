package dev.maxenceng.lazyclaude.review.domain;

/**
 * How many comments other people can see on a pull request: its conversation comments plus every comment of its review threads, the
 * replies included. Your own pending review's comments are left out, since nobody else sees them, and a review's summary is not a comment.
 */
public record CommentCount(int value) {}

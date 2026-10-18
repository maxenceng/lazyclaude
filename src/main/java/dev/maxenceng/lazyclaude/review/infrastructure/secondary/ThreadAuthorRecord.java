package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

/** Who wrote a comment as the threads query writes it; null for an account that is gone. */
record ThreadAuthorRecord(String login) {}

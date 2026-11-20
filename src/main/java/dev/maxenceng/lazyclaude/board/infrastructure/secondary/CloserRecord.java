package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

/**
 * What closed an issue, as GraphQL writes it: a pull request's head branch and whether it merged, both null where the closer is not a
 * pull request.
 */
record CloserRecord(String headRefName, Boolean merged) {}

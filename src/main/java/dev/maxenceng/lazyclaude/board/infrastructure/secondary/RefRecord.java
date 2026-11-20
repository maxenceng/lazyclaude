package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

/** A git ref as GraphQL writes it; only its name is read. */
record RefRecord(String name) {}

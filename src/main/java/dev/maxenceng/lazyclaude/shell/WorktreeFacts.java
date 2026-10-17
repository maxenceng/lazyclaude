package dev.maxenceng.lazyclaude.shell;

/** What the ticket detail says of a worktree: where it is, and its state, which is empty when only a reason is given. */
record WorktreeFacts(String place, String state) {}

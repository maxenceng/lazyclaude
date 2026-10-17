package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** What was last read of a ticket's worktree, and whose it is, so a read that lands after another ticket was selected shows nowhere. */
record ShownWorktree(LinkedTicketId ticket, WorktreeView view) {}

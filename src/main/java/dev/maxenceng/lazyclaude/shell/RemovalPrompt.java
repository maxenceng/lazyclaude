package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HeldWorktreeView;

/** The question on the status line: remove this ticket's worktree? */
record RemovalPrompt(TicketView ticket, HeldWorktreeView worktree) {}

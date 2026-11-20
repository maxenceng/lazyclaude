package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;

/** The question on the status line: remove this ticket's worktree at this path? */
record RemovalPrompt(TicketView ticket, Path worktree) {}

package dev.maxenceng.lazyclaude.board.published;

/** What a ticket is called: its first `# ` heading, or nothing when it has none — never a stand-in. */
public sealed interface TicketTitle permits Title, NoTitle {}

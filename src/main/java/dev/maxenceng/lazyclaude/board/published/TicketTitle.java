package dev.maxenceng.lazyclaude.board.published;

/** What a ticket is called, taken from its body, or nothing when the body gives none — never a stand-in. */
public sealed interface TicketTitle permits Title, NoTitle {}

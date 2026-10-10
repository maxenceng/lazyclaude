package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.FileName;

/** One backlog file that claims to be a ticket: read into one, or refused with a reason. */
public sealed interface BacklogEntry permits TicketFile, UnreadableTicket {

    FileName file();
}

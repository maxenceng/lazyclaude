package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.TicketBody;

/** The project has no ticket template, so a new ticket starts empty. */
public record NoTemplate() implements TicketTemplate {

    @Override
    public TicketBody sections() {
        return new NoBody();
    }
}

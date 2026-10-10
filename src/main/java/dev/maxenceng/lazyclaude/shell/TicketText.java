package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.TypeName;

/** How the shell words a ticket: a ticket with no title is shown under its id, and a missing field is left out. */
final class TicketText {

    private static final String STATUS_PREFIX = "status ";
    private static final String FACT_SEPARATOR = " · ";

    private TicketText() {}

    static String title(TicketView ticket) {
        return switch (ticket.title()) {
            case Title title -> title.value();
            case NoTitle noTitle -> ticket.id().value();
        };
    }

    static String facts(TicketView ticket) {
        List<String> facts = new ArrayList<>(List.of(STATUS_PREFIX + ticket.status().label()));
        switch (ticket.context()) {
            case ContextName context -> facts.add(context.value());
            case NoContext noContext -> {}
        }
        switch (ticket.type()) {
            case TypeName type -> facts.add(type.value());
            case NoType noType -> {}
        }
        return String.join(FACT_SEPARATOR, facts);
    }
}

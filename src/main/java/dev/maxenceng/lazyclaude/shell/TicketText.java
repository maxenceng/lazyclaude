package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.FrontmatterEntryView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.TypeName;

/**
 * How the shell words a ticket: a ticket with no title is shown under its id, a missing field is left
 * out, and the frontmatter keys the facts line already carries are not listed again.
 */
final class TicketText {

    private static final String FACT_SEPARATOR = " · ";
    private static final Set<String> FACT_KEYS = Set.of("id", "status", "context", "type");
    private static final String KEY_GAP = "  ";

    private TicketText() {}

    static String title(TicketView ticket) {
        return switch (ticket.title()) {
            case Title title -> title.value();
            case NoTitle noTitle -> ticket.id().value();
        };
    }

    static String facts(TicketView ticket) {
        List<String> facts = new ArrayList<>(List.of(ticket.id().value(), ticket.status().label()));
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

    static List<String> keyRows(TicketView ticket) {
        List<FrontmatterEntryView> others = ticket.frontmatter().stream()
                .filter(entry -> !FACT_KEYS.contains(entry.key().name()))
                .toList();
        int keyWidth = others.stream().mapToInt(entry -> entry.key().name().length()).max().orElse(0);
        return others.stream().map(entry -> keyRow(entry, keyWidth)).toList();
    }

    private static String keyRow(FrontmatterEntryView entry, int keyWidth) {
        String key = entry.key().name();
        return key + " ".repeat(keyWidth - key.length()) + KEY_GAP + entry.value().text();
    }
}

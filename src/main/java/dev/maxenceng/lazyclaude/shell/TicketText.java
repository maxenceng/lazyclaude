package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.FrontmatterEntryView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.TypeName;
import dev.tamboui.style.Style;

/**
 * How the shell words a ticket: a ticket with no title is shown under its id, a missing field is left
 * out, and the other frontmatter keys line up under their longest name.
 */
final class TicketText {

    private static final String FACT_SEPARATOR = " · ";
    private static final String KEY_GAP = "  ";
    private static final String RULE = "─";
    private static final String NO_BODY = "no body";

    private TicketText() {}

    static String title(TicketView ticket) {
        return switch (ticket.title()) {
            case Title title -> title.value();
            case NoTitle noTitle -> ticket.id().value();
        };
    }

    /** The rows above a ticket's body: the facts line, the other frontmatter keys, a rule across {@code width}, and "no body" when it has none. */
    static List<DetailRow> head(TicketView ticket, int width) {
        List<DetailRow> head = new ArrayList<>();
        head.add(new DetailRow(facts(ticket), Style.EMPTY.bold()));
        keyRows(ticket).forEach(key -> head.add(new DetailRow(key, Style.EMPTY)));
        head.add(new DetailRow(RULE.repeat(width), Style.EMPTY.dim()));
        if (ticket.body() instanceof NoBody) {
            head.add(new DetailRow(NO_BODY, Style.EMPTY.dim()));
        }
        return head;
    }

    static Optional<String> markdown(TicketView ticket) {
        return switch (ticket.body()) {
            case Body text -> Optional.of(text.value());
            case NoBody none -> Optional.empty();
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
        List<FrontmatterEntryView> others = ticket.frontmatter();
        int keyWidth = others.stream().mapToInt(entry -> entry.key().name().length()).max().orElse(0);
        return others.stream().map(entry -> keyRow(entry, keyWidth)).toList();
    }

    private static String keyRow(FrontmatterEntryView entry, int keyWidth) {
        String key = entry.key().name();
        return key + " ".repeat(keyWidth - key.length()) + KEY_GAP + entry.value().text();
    }
}

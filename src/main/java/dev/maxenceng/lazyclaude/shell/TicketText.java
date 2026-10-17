package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
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

    /**
     * The rows above a ticket's body: the facts line, the other frontmatter keys, the rows about its pull request in the same key
     * column, a rule across {@code width}, and "no body" when it has none.
     */
    static List<DetailRow> head(TicketView ticket, PullRequestsView pullRequests, int width) {
        return Stream.of(
                        Stream.of(new DetailRow(facts(ticket), Style.EMPTY.bold())),
                        keyRows(ticket).stream().map(key -> new DetailRow(key, Style.EMPTY)),
                        PullRequestText.rows(ticket, pullRequests, keyWidth(ticket), width).stream(),
                        Stream.of(new DetailRow(RULE.repeat(width), Style.EMPTY.dim())),
                        Included.when(ticket.body() instanceof NoBody, new DetailRow(NO_BODY, Style.EMPTY.dim())))
                .flatMap(Function.identity())
                .toList();
    }

    static Optional<String> markdown(TicketView ticket) {
        return switch (ticket.body()) {
            case Body text -> Optional.of(text.value());
            case NoBody none -> Optional.empty();
        };
    }

    static String facts(TicketView ticket) {
        Stream<String> context = switch (ticket.context()) {
            case ContextName name -> Stream.of(name.value());
            case NoContext noContext -> Stream.empty();
        };
        Stream<String> type = switch (ticket.type()) {
            case TypeName name -> Stream.of(name.value());
            case NoType noType -> Stream.empty();
        };
        return Stream.of(Stream.of(ticket.id().value(), ticket.status().label()), context, type)
                .flatMap(Function.identity())
                .collect(Collectors.joining(FACT_SEPARATOR));
    }

    static List<String> keyRows(TicketView ticket) {
        return ticket.frontmatter().stream().map(entry -> keyRow(entry, keyWidth(ticket))).toList();
    }

    /** The width of the key column: the longest frontmatter key, or the longest key of the pull request rows when that is wider. */
    private static int keyWidth(TicketView ticket) {
        int longest = ticket.frontmatter().stream()
                .mapToInt(entry -> entry.key().name().length())
                .max()
                .orElse(0);
        return Math.max(longest, PullRequestText.keyWidth());
    }

    private static String keyRow(FrontmatterEntryView entry, int keyWidth) {
        String key = entry.key().name();
        return key + " ".repeat(keyWidth - key.length()) + KEY_GAP + entry.value().text();
    }
}

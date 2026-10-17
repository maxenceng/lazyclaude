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
    private static final String WORKTREE_KEY = "worktree";
    private static final String NO_KEY = "";

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
    static List<DetailRow> head(TicketView ticket, PullRequestsView pullRequests, Optional<WorktreeFacts> worktree, int width) {
        return Stream.of(
                        Stream.of(new DetailRow(facts(ticket), Style.EMPTY.bold())),
                        keyRows(ticket, worktree, width).stream().map(key -> new DetailRow(key, Style.EMPTY)),
                        PullRequestText.rows(ticket, pullRequests, keyWidth(ticket, worktree), width).stream(),
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

    /** The frontmatter rows, then the worktree's under them when the ticket has one to show. */
    private static List<String> keyRows(TicketView ticket, Optional<WorktreeFacts> worktree, int width) {
        int keyWidth = keyWidth(ticket, worktree);
        return Stream.concat(
                        ticket.frontmatter().stream().map(entry -> keyRow(entry, keyWidth)),
                        worktree.stream().flatMap(facts -> worktreeRows(facts, keyWidth, width).stream()))
                .toList();
    }

    /** The width of the key column: the longest frontmatter key, or the longest key of the pull request rows when that is wider. */
    private static int keyWidth(TicketView ticket, Optional<WorktreeFacts> worktree) {
        int longest = ticket.frontmatter().stream()
                .mapToInt(entry -> entry.key().name().length())
                .max()
                .orElse(0);
        int worktreeKey = worktree.isPresent() ? WORKTREE_KEY.length() : 0;
        return Math.max(Math.max(longest, worktreeKey), PullRequestText.keyWidth());
    }

    /** The worktree's place and state on one row when they fit, else the state on a row under the place, so a narrow view never cuts it off. */
    private static List<String> worktreeRows(WorktreeFacts facts, int keyWidth, int width) {
        String whole = keyed(WORKTREE_KEY, facts.place() + KEY_GAP + facts.state(), keyWidth);
        if (facts.state().isEmpty()) {
            return List.of(keyed(WORKTREE_KEY, facts.place(), keyWidth));
        }
        return whole.length() <= width
                ? List.of(whole)
                : List.of(keyed(WORKTREE_KEY, facts.place(), keyWidth), keyed(NO_KEY, facts.state(), keyWidth));
    }

    private static String keyRow(FrontmatterEntryView entry, int keyWidth) {
        return keyed(entry.key().name(), entry.value().text(), keyWidth);
    }

    private static String keyed(String key, String value, int keyWidth) {
        return key + " ".repeat(keyWidth - key.length()) + KEY_GAP + value;
    }
}

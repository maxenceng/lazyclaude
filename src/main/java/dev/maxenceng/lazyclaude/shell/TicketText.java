package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TypeName;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.tamboui.style.Style;

/**
 * How the shell words a ticket: its number after a number sign, a missing field left out, and the issue's other labels in a row of the key
 * column beside the pull request's and the worktree's.
 */
final class TicketText {

    private static final String PAD = " ";
    private static final String FACT_SEPARATOR = " · ";
    private static final String KEY_GAP = "  ";
    private static final String RULE = "─";
    private static final String NO_BODY = "no body";
    private static final String WORKTREE_KEY = "worktree";
    private static final String LABELS_KEY = "labels";
    private static final String LABEL_SEPARATOR = ", ";
    private static final String NUMBER_SIGN = "#";
    private static final String NO_KEY = "";

    private TicketText() {}

    static String title(TicketView ticket) {
        return ticket.title().value();
    }

    /** A ticket's id as GitHub writes an issue's number. */
    static String number(TicketId id) {
        return number(id.value());
    }

    /** A ticket's id, as the sessions name it, written as GitHub writes an issue's number. */
    static String number(String id) {
        return NUMBER_SIGN + id;
    }

    /**
     * The rows above a ticket's body: the facts line, the labels, the rows about its pull request in the same key
     * column, a rule across {@code width}, and "no body" when it has none.
     */
    static List<DetailRow> head(
            TicketView ticket,
            PullRequestsView pullRequests,
            Optional<PendingReviewView> pending,
            Optional<WorktreeFacts> worktree,
            int width) {
        return Stream.of(
                        Stream.of(new DetailRow(facts(ticket), Style.EMPTY.bold())),
                        keyRows(ticket, worktree, width).stream().map(key -> new DetailRow(key, Style.EMPTY)),
                        PullRequestText.rows(ticket, pullRequests, pending, keyWidth(ticket, worktree), width).stream(),
                        Stream.of(new DetailRow(RULE.repeat(width), Style.EMPTY.dim())),
                        Included.when(ticket.body() instanceof NoBody, new DetailRow(NO_BODY, Style.EMPTY.dim())))
                .flatMap(Function.identity())
                .toList();
    }

    static Optional<String> markdown(TicketView ticket) {
        return switch (ticket.body()) {
            case Body text -> Optional.of(text.value());
            case NoBody _ -> Optional.empty();
        };
    }

    static String facts(TicketView ticket) {
        Stream<String> context = switch (ticket.context()) {
            case ContextName name -> Stream.of(name.value());
            case NoContext _ -> Stream.empty();
        };
        Stream<String> type = switch (ticket.type()) {
            case TypeName name -> Stream.of(name.value());
            case NoType _ -> Stream.empty();
        };
        return Stream.of(Stream.of(number(ticket.id()), ticket.status().label()), context, type)
                .flatMap(Function.identity())
                .collect(Collectors.joining(FACT_SEPARATOR));
    }

    /** The labels row when the ticket has labels to show, then the worktree's under it when the ticket has one to show. */
    private static List<String> keyRows(TicketView ticket, Optional<WorktreeFacts> worktree, int width) {
        int keyWidth = keyWidth(ticket, worktree);
        return Stream.concat(
                        labelsRow(ticket, keyWidth).stream(),
                        worktree.stream().flatMap(facts -> worktreeRows(facts, keyWidth, width).stream()))
                .toList();
    }

    private static Optional<String> labelsRow(TicketView ticket, int keyWidth) {
        return Optional.of(ticket.otherLabels().stream().map(LabelName::value).collect(Collectors.joining(LABEL_SEPARATOR)))
                .filter(labels -> !labels.isEmpty())
                .map(labels -> keyed(LABELS_KEY, labels, keyWidth));
    }

    /** The width of the key column: the longest of the labels, worktree and pull request keys. */
    private static int keyWidth(TicketView ticket, Optional<WorktreeFacts> worktree) {
        int labelsKey = ticket.otherLabels().isEmpty() ? 0 : LABELS_KEY.length();
        int worktreeKey = worktree.isPresent() ? WORKTREE_KEY.length() : 0;
        return Math.max(Math.max(labelsKey, worktreeKey), PullRequestText.keyWidth());
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

    private static String keyed(String key, String value, int keyWidth) {
        return key + PAD.repeat(keyWidth - key.length()) + KEY_GAP + value;
    }
}

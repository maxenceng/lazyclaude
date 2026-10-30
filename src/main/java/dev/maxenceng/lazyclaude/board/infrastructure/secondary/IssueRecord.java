package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketContext;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.TicketType;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.TypeName;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/**
 * One issue as {@code gh issue list --json} writes it, null where the JSON lacks it, and the rules that read it as a ticket (ADR 0052):
 * an open issue is the status its one {@code status:} label names, a draft without one; a closed one is done when it closed as
 * completed; a ticket past draft has exactly one {@code context:} and one {@code type:} label.
 */
record IssueRecord(Integer number, String title, String body, List<LabelRecord> labels, String state, String stateReason) {

    private static final String NUMBER = "number";
    private static final String TITLE = "title";
    private static final String STATE = "state";
    private static final String CLOSED = "CLOSED";
    private static final String COMPLETED = "COMPLETED";
    private static final String NOT_ASKED = "";
    private static final String STATUS_PREFIX = "status:";
    private static final String CONTEXT_PREFIX = "context:";
    private static final String TYPE_PREFIX = "type:";
    private static final String LIST_SEPARATOR = ", ";
    private static final String CLOSED_AS = "closed as %s";
    private static final String UNKNOWN_STATUS = "status label '%s' is not one of %s";
    private static final String TWO_STATUSES = "has more than one status label: %s";
    private static final String NOT_ONE = "needs exactly one %s label, has %s";
    private static final String NONE = "none";
    private static final String SEVERAL = "several";
    private static final String NON_SLUG = "[^a-z0-9]+";
    private static final Pattern NON_SLUG_RUN = Pattern.compile(NON_SLUG);
    private static final String SLUG_JOIN = "-";
    private static final String EDGE_JOINS = "^-+|-+$";
    private static final String BRANCH = "%s-%s";

    /** The fields an issue cannot be read without that the JSON lacks. */
    MissingFields missingFields() {
        return new MissingFields().absent(NUMBER, number).blank(TITLE, title).blank(STATE, state);
    }

    /** Only once {@link #missingFields()} names none. */
    BacklogEntry entry() {
        TicketId id = new TicketId(String.valueOf(number));
        return refusal().<BacklogEntry>map(why -> UnreadableTicket.builder().id(id).reason(new Reason(why)).build()).orElseGet(() -> ticket(id));
    }

    private Optional<String> refusal() {
        if (isClosed()) {
            return Optional.of(stateReason)
                    .filter(why -> !why.equals(COMPLETED) && !why.equals(NOT_ASKED))
                    .map(why -> CLOSED_AS.formatted(why.toLowerCase(Locale.ROOT).replace('_', ' ')))
                    .or(this::missingLabel);
        }
        return statusRefusal().or(this::missingLabel);
    }

    private Optional<String> statusRefusal() {
        List<String> named = valuesOf(STATUS_PREFIX);
        if (named.size() > 1) {
            return Optional.of(TWO_STATUSES.formatted(String.join(LIST_SEPARATOR, named)));
        }
        return named.stream()
                .filter(label -> Status.find(label).isEmpty())
                .findFirst()
                .map(label -> UNKNOWN_STATUS.formatted(label, statusLabels()));
    }

    /** A ticket past draft that does not carry exactly one context and one type, and a draft that carries several. */
    private Optional<String> missingLabel() {
        boolean strict = status() != Status.DRAFT;
        return Stream.of(CONTEXT_PREFIX, TYPE_PREFIX)
                .filter(prefix -> strict ? valuesOf(prefix).size() != 1 : valuesOf(prefix).size() > 1)
                .findFirst()
                .map(prefix -> NOT_ONE.formatted(prefix, valuesOf(prefix).isEmpty() ? NONE : SEVERAL));
    }

    private Ticket ticket(TicketId id) {
        return Ticket.builder()
                .id(id)
                .branch(branch())
                .status(status())
                .context(context())
                .type(type())
                .title(new Title(title))
                .otherLabels(otherLabels())
                .body(textBody())
                .build();
    }

    private Status status() {
        return isClosed()
                ? Status.DONE
                : valuesOf(STATUS_PREFIX).stream().findFirst().flatMap(Status::find).orElse(Status.DRAFT);
    }

    private TicketContext context() {
        return valuesOf(CONTEXT_PREFIX).stream().findFirst().<TicketContext>map(ContextName::new).orElseGet(NoContext::new);
    }

    private TicketType type() {
        return valuesOf(TYPE_PREFIX).stream().findFirst().<TicketType>map(TypeName::new).orElseGet(NoType::new);
    }

    private List<LabelName> otherLabels() {
        return names().stream()
                .filter(name -> Stream.of(STATUS_PREFIX, CONTEXT_PREFIX, TYPE_PREFIX).noneMatch(name::startsWith))
                .map(LabelName::new)
                .toList();
    }

    private TicketBody textBody() {
        return body == null || body.isBlank() ? new NoBody() : new Body(body);
    }

    // deferred: the branch is the number and the title's slug, as `gh issue develop` names one by default, not read back from linkedBranches — close it when a ticket's branch is created under another name.
    private TicketBranch branch() {
        String slug = NON_SLUG_RUN.matcher(title.toLowerCase(Locale.ROOT)).replaceAll(SLUG_JOIN).replaceAll(EDGE_JOINS, NOT_ASKED);
        return new TicketBranch(slug.isEmpty() ? String.valueOf(number) : BRANCH.formatted(number, slug));
    }

    private boolean isClosed() {
        return CLOSED.equals(state);
    }

    private List<String> names() {
        return labels == null ? List.of() : labels.stream().map(LabelRecord::name).filter(name -> name != null && !name.isBlank()).toList();
    }

    private List<String> valuesOf(String prefix) {
        return names().stream().filter(name -> name.startsWith(prefix)).map(name -> name.substring(prefix.length())).toList();
    }

    private static String statusLabels() {
        return Arrays.stream(Status.values()).map(Status::label).collect(Collectors.joining(LIST_SEPARATOR));
    }
}

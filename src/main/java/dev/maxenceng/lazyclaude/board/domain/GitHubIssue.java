package dev.maxenceng.lazyclaude.board.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.LabelName;
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
import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * The GitHub issue that stores a ticket, and the rules that read it as one (ADR 0052): an open issue is the status its one {@code status:}
 * label names, a draft without one; a closed one is done when it closed as completed, unreadable otherwise; a ticket from {@code todo}
 * until done has exactly one {@code context:} and one {@code type:} label, and a draft or a done one has at most one of each.
 */
public record GitHubIssue(TicketId id, TicketBranch branch, Title title, TicketBody body, IssueState state, List<LabelName> labels) {

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

    public GitHubIssue {
        Assert.notNull("id", id);
        Assert.notNull("branch", branch);
        Assert.notNull("title", title);
        Assert.notNull("body", body);
        Assert.notNull("state", state);
        Assert.notNull("labels", labels);
    }

    /** The ticket the issue is, or why the board refuses it as one. */
    public BacklogEntry entry() {
        return refusal().<BacklogEntry>map(why -> UnreadableTicket.builder().id(id).reason(new Reason(why)).build()).orElseGet(this::ticket);
    }

    private Optional<String> refusal() {
        return switch (state) {
            case ClosedIssue(ClosedReason reason) when !reason.completed() -> Optional.of(CLOSED_AS.formatted(reason.phrase()));
            case ClosedIssue _ -> labelCountRefusal();
            case OpenIssue _ -> statusRefusal().or(this::labelCountRefusal);
        };
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

    /** A ticket from {@code todo} until done that does not carry exactly one context and one type, and any that carries several. */
    private Optional<String> labelCountRefusal() {
        Status status = status();
        boolean strict = status != Status.DRAFT && !status.finished();
        return Stream.of(CONTEXT_PREFIX, TYPE_PREFIX)
                .filter(prefix -> strict ? valuesOf(prefix).size() != 1 : valuesOf(prefix).size() > 1)
                .findFirst()
                .map(prefix -> NOT_ONE.formatted(prefix, valuesOf(prefix).isEmpty() ? NONE : SEVERAL));
    }

    private Ticket ticket() {
        return Ticket.builder()
                .id(id)
                .branch(branch)
                .status(status())
                .context(context())
                .type(type())
                .title(title)
                .otherLabels(otherLabels())
                .body(body)
                .build();
    }

    private Status status() {
        return state instanceof ClosedIssue
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
        return labels.stream()
                .filter(label -> Stream.of(STATUS_PREFIX, CONTEXT_PREFIX, TYPE_PREFIX).noneMatch(label.value()::startsWith))
                .toList();
    }

    private List<String> valuesOf(String prefix) {
        return labels.stream()
                .map(LabelName::value)
                .filter(name -> name.startsWith(prefix))
                .map(name -> name.substring(prefix.length()))
                .toList();
    }

    private static String statusLabels() {
        return Arrays.stream(Status.values()).map(Status::label).collect(Collectors.joining(LIST_SEPARATOR));
    }

    public static GitHubIssueBuilder builder() {
        return new GitHubIssueBuilder();
    }

    public static final class GitHubIssueBuilder {
        private TicketId id;
        private TicketBranch branch;
        private Title title;
        private TicketBody body;
        private IssueState state;
        private List<LabelName> labels = List.of();

        private GitHubIssueBuilder() {}

        public GitHubIssueBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public GitHubIssueBuilder branch(TicketBranch branch) {
            this.branch = branch;
            return this;
        }

        public GitHubIssueBuilder title(Title title) {
            this.title = title;
            return this;
        }

        public GitHubIssueBuilder body(TicketBody body) {
            this.body = body;
            return this;
        }

        public GitHubIssueBuilder state(IssueState state) {
            this.state = state;
            return this;
        }

        public GitHubIssueBuilder labels(List<LabelName> labels) {
            this.labels = labels;
            return this;
        }

        public GitHubIssue build() {
            return new GitHubIssue(id, branch, title, body, state, labels);
        }
    }
}

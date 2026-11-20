package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.ClosedAt;
import dev.maxenceng.lazyclaude.board.domain.ClosedIssue;
import dev.maxenceng.lazyclaude.board.domain.ClosedReason;
import dev.maxenceng.lazyclaude.board.domain.GitHubIssue;
import dev.maxenceng.lazyclaude.board.domain.IssueState;
import dev.maxenceng.lazyclaude.board.domain.OpenIssue;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/**
 * One issue as {@code gh api graphql} writes it, null where the JSON lacks it. The domain's {@link GitHubIssue} decides what ticket it
 * is (ADR 0043); this reads the fields, and names the branch GitHub links to it, else the head of the merged pull request that closed
 * it, else as {@code gh issue develop} would (ADR 0118, ADR 0121).
 */
record IssueRecord(
        Integer number,
        String title,
        String body,
        NodesRecord<LabelRecord> labels,
        String state,
        String stateReason,
        String closedAt,
        NodesRecord<LinkedBranchRecord> linkedBranches,
        NodesRecord<ClosedEventRecord> timelineItems) {

    private static final String NUMBER = "number";
    private static final String TITLE = "title";
    private static final String STATE = "state";
    private static final String CLOSED = "CLOSED";
    private static final String CLOSED_AT = "closedAt";
    private static final String UNREAD_TIME = "gh api graphql wrote a closedAt that is not a time: %s";
    private static final String NO_REASON = "";
    private static final String NON_SLUG = "[^a-z0-9]+";
    private static final Pattern NON_SLUG_RUN = Pattern.compile(NON_SLUG);
    private static final String SLUG_JOIN = "-";
    private static final String EDGE_JOINS = "^-+|-+$";
    private static final String NO_JOIN = "";
    private static final String BRANCH = "%s-%s";

    /** The fields an issue cannot be read without that the JSON lacks. */
    MissingFields missingFields() {
        MissingFields fields = new MissingFields().absent(NUMBER, number).blank(TITLE, title).blank(STATE, state);
        return CLOSED.equals(state) ? fields.blank(CLOSED_AT, closedAt) : fields;
    }

    /** Only once {@link #missingFields()} names none. */
    GitHubIssue issue() {
        return GitHubIssue.builder()
                .id(new TicketId(String.valueOf(number)))
                .branch(branch())
                .title(new Title(title))
                .body(textBody())
                .state(issueState())
                .labels(names())
                .build();
    }

    private IssueState issueState() {
        return CLOSED.equals(state)
                ? ClosedIssue.builder()
                        .reason(new ClosedReason(stateReason == null ? NO_REASON : stateReason))
                        .closedAt(new ClosedAt(closedTime()))
                        .build()
                : new OpenIssue();
    }

    private Instant closedTime() {
        try {
            return Instant.parse(closedAt);
        } catch (DateTimeParseException e) {
            throw new BacklogUnavailableException(UNREAD_TIME.formatted(closedAt), e);
        }
    }

    private TicketBody textBody() {
        return body == null || body.isBlank() ? new NoBody() : new Body(body);
    }

    private TicketBranch branch() {
        return linkedBranch().or(this::closingPullRequestHead).orElseGet(this::slugBranch);
    }

    private Optional<TicketBranch> linkedBranch() {
        return linkedBranches == null
                ? Optional.empty()
                : linkedBranches.listed().stream()
                        .findFirst()
                        .map(LinkedBranchRecord::ref)
                        .map(RefRecord::name)
                        .map(TicketBranch::new);
    }

    /** Only a closed issue's: a reopened one is worked again on a branch of its own, not on the one already merged. */
    private Optional<TicketBranch> closingPullRequestHead() {
        return !CLOSED.equals(state) || timelineItems == null
                ? Optional.empty()
                : timelineItems.listed().stream()
                        .reduce((earlier, later) -> later)
                        .map(ClosedEventRecord::closer)
                        .filter(closer -> Boolean.TRUE.equals(closer.merged()))
                        .map(CloserRecord::headRefName)
                        .filter(head -> !head.isBlank())
                        .map(TicketBranch::new);
    }

    private TicketBranch slugBranch() {
        String slug = NON_SLUG_RUN.matcher(title.toLowerCase(Locale.ROOT)).replaceAll(SLUG_JOIN).replaceAll(EDGE_JOINS, NO_JOIN);
        return new TicketBranch(slug.isEmpty() ? String.valueOf(number) : BRANCH.formatted(number, slug));
    }

    private List<LabelName> names() {
        return labels == null
                ? List.of()
                : labels.listed().stream().map(LabelRecord::name).filter(name -> name != null && !name.isBlank()).map(LabelName::new).toList();
    }
}

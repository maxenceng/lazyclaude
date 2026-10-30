package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
 * One issue as {@code gh issue list --json} writes it, null where the JSON lacks it. The domain's {@link GitHubIssue} decides what ticket it
 * is (ADR 0043); this reads the fields, and names the branch as {@code gh issue develop} would.
 */
record IssueRecord(Integer number, String title, String body, List<LabelRecord> labels, String state, String stateReason) {

    private static final String NUMBER = "number";
    private static final String TITLE = "title";
    private static final String STATE = "state";
    private static final String CLOSED = "CLOSED";
    private static final String NO_REASON = "";
    private static final String NON_SLUG = "[^a-z0-9]+";
    private static final Pattern NON_SLUG_RUN = Pattern.compile(NON_SLUG);
    private static final String SLUG_JOIN = "-";
    private static final String EDGE_JOINS = "^-+|-+$";
    private static final String NO_JOIN = "";
    private static final String BRANCH = "%s-%s";

    /** The fields an issue cannot be read without that the JSON lacks. */
    MissingFields missingFields() {
        return new MissingFields().absent(NUMBER, number).blank(TITLE, title).blank(STATE, state);
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
        return CLOSED.equals(state) ? new ClosedIssue(new ClosedReason(stateReason == null ? NO_REASON : stateReason)) : new OpenIssue();
    }

    private TicketBody textBody() {
        return body == null || body.isBlank() ? new NoBody() : new Body(body);
    }

    // deferred: the branch is the number and the title's slug, as `gh issue develop` names one by default, not read back from linkedBranches — close it when a ticket's branch is created under another name.
    private TicketBranch branch() {
        String slug = NON_SLUG_RUN.matcher(title.toLowerCase(Locale.ROOT)).replaceAll(SLUG_JOIN).replaceAll(EDGE_JOINS, NO_JOIN);
        return new TicketBranch(slug.isEmpty() ? String.valueOf(number) : BRANCH.formatted(number, slug));
    }

    private List<LabelName> names() {
        return labels == null
                ? List.of()
                : labels.stream().map(LabelRecord::name).filter(name -> name != null && !name.isBlank()).map(LabelName::new).toList();
    }
}

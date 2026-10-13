package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.CheckView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.CheckingPullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.OpenPullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.UnavailablePullRequestsView;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;
import dev.maxenceng.lazyclaude.review.published.CiResult;
import dev.maxenceng.lazyclaude.review.published.Failed;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.NoCiResult;
import dev.maxenceng.lazyclaude.review.published.Passed;
import dev.maxenceng.lazyclaude.review.published.Pending;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * How the shell words a ticket's pull request: the two cells at the right edge of its board row, a dim mark for a draft pull
 * request or one without checks and then the glyph of its CI result, and the rows of its detail. A ticket's pull request is the open
 * one whose head is its ticket branch.
 */
final class PullRequestText {

    private static final String DRAFT_GLYPH = "⊙";
    private static final String PASSED_GLYPH = "✓";
    private static final String FAILED_GLYPH = "✗";
    private static final String PENDING_GLYPH = "◌";
    private static final String PR_KEY = "PR";
    private static final String CHECKS_KEY = "checks";
    private static final String KEY_GAP = "  ";
    private static final String NO_KEY = "";
    private static final String GLYPH_GAP = " ";
    private static final String NONE = "none";
    private static final String CHECKING = "checking…";
    private static final String UNKNOWN = "unknown (%s)";
    private static final String NUMBER = "#%d ";
    private static final String DRAFT_SUFFIX = " (draft PR)";

    private PullRequestText() {}

    /** The width of the key column the PR rows need, so that the frontmatter rows can be aligned with them. */
    static int keyWidth() {
        return CHECKS_KEY.length();
    }

    /** The cells at the right edge of a ticket's row: none without a pull request, else the draft slot and then the CI slot. */
    static List<RowCell> column(TicketView ticket, PullRequestsView pullRequests) {
        return pullRequestOf(ticket, pullRequests)
                .map(pullRequest -> List.of(draftCell(pullRequest), ciCell(pullRequest.result())))
                .orElse(List.of());
    }

    /** The rows of a ticket's detail about its pull request, with their keys padded to {@code keyWidth}, in {@code width} cells. */
    static List<DetailRow> rows(TicketView ticket, PullRequestsView pullRequests, int keyWidth, int width) {
        return switch (pullRequests) {
            case CheckingPullRequestsView checking -> List.of(dimRow(PR_KEY, CHECKING, keyWidth));
            case UnavailablePullRequestsView unavailable ->
                List.of(dimRow(PR_KEY, UNKNOWN.formatted(unavailable.reason().value()), keyWidth));
            case OpenPullRequestsView open ->
                pullRequestOf(ticket, open)
                        .map(pullRequest -> openRows(pullRequest, keyWidth, width))
                        .orElseGet(() -> List.of(dimRow(PR_KEY, NONE, keyWidth)));
        };
    }

    private static List<DetailRow> openRows(PullRequestView pullRequest, int keyWidth, int width) {
        List<CheckView> checks = pullRequest.checks();
        Stream<DetailRow> checkRows = IntStream.range(0, checks.size())
                .mapToObj(index -> new DetailRow(
                        keyed(index == 0 ? CHECKS_KEY : NO_KEY, checkLine(checks.get(index)), keyWidth), Style.EMPTY));
        return Stream.concat(Stream.of(new DetailRow(keyed(PR_KEY, heading(pullRequest, width - keyWidth - KEY_GAP.length()), keyWidth), Style.EMPTY)), checkRows)
                .toList();
    }

    private static DetailRow dimRow(String key, String value, int keyWidth) {
        return new DetailRow(keyed(key, value, keyWidth), Style.EMPTY.dim());
    }

    private static String keyed(String key, String value, int keyWidth) {
        return key + " ".repeat(keyWidth - key.length()) + KEY_GAP + value;
    }

    /** The number, the title and the draft suffix in {@code room} cells: a long title gives way to the suffix, not the reverse. */
    private static String heading(PullRequestView pullRequest, int room) {
        String number = NUMBER.formatted(pullRequest.number().value());
        String suffix = pullRequest.draft() ? DRAFT_SUFFIX : NO_KEY;
        int titleRoom = Math.max(1, room - number.length() - suffix.length());
        return number + RowsElement.fit(pullRequest.title().value(), titleRoom) + suffix;
    }

    private static String checkLine(CheckView check) {
        return glyph(check.outcome()) + GLYPH_GAP + check.name().value();
    }

    private static String glyph(CheckOutcome outcome) {
        return switch (outcome) {
            case FAILED -> FAILED_GLYPH;
            case PENDING -> PENDING_GLYPH;
            case PASSED -> PASSED_GLYPH;
        };
    }

    private static RowCell draftCell(PullRequestView pullRequest) {
        return pullRequest.draft() || pullRequest.result() instanceof NoCiResult
                ? new RowCell(DRAFT_GLYPH, Style.EMPTY.dim())
                : RowCell.blank();
    }

    private static RowCell ciCell(CiResult result) {
        return switch (result) {
            case Passed passed -> new RowCell(PASSED_GLYPH, Style.EMPTY.fg(Color.GREEN));
            case Failed failed -> new RowCell(FAILED_GLYPH, Style.EMPTY.fg(Color.RED));
            case Pending pending -> new RowCell(PENDING_GLYPH, Style.EMPTY.fg(Color.YELLOW));
            case NoCiResult none -> RowCell.blank();
        };
    }

    private static Optional<PullRequestView> pullRequestOf(TicketView ticket, PullRequestsView pullRequests) {
        return pullRequests instanceof OpenPullRequestsView open
                ? open.on(new HeadBranch(ticket.branch().value()))
                : Optional.empty();
    }
}

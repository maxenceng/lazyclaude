package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.DiffFileView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.LoadingDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.LoadingThreadsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReadDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReadThreadsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.UnavailableDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.UnavailableThreadsView;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.RenamedFile;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;
import dev.tamboui.style.Style;

/** How the review words its Files and Comments panels and what stands in the main view while there is no diff to show. */
final class ReviewText {

    private static final String LOADING = "loading…";
    private static final String NO_CHANGES = "no changes";
    private static final String NO_THREADS = "no threads";
    private static final String PR_UNKNOWN = "PR unknown (%s)";
    private static final String DIFF_TOO_LARGE = "diff too large for GitHub (%s)";
    private static final String THREADS_UNKNOWN = "threads unknown (%s)";
    private static final String OUTDATED = "outdated";
    private static final String OPEN_MARK = "◆ ";
    private static final String NO_MARK = "  ";
    private static final String OPEN_GLYPH = "◆";
    private static final String LETTER_GAP = " ";
    private static final String PLACED = "%s:%s %s";
    private static final String PLACED_WITHOUT_AUTHOR = "%s:%s";
    private static final String OLD_SIDE = "-";
    private static final String OUTDATED_ROW = "%s  %s";
    private static final String PATH_SEPARATOR = "/";
    private static final String RENAMED = "%s → %s";

    private ReviewText() {}

    /** What the main view says instead of the diff while it is loading, cannot be read or holds nothing; none once there are files. */
    static Optional<String> diffNotice(PullRequestDiffView diff) {
        return switch (diff) {
            case LoadingDiffView _ -> Optional.of(LOADING);
            case UnavailableDiffView unavailable ->
                Optional.of((unavailable.tooLarge() ? DIFF_TOO_LARGE : PR_UNKNOWN).formatted(unavailable.reason().value()));
            case ReadDiffView read -> read.files().isEmpty() ? Optional.of(NO_CHANGES) : Optional.empty();
        };
    }

    /** What Comments says instead of its threads while they are loading, cannot be read or there are none. */
    static Optional<String> commentsNotice(ThreadsView threads) {
        return switch (threads) {
            case LoadingThreadsView _ -> Optional.of(LOADING);
            case UnavailableThreadsView unavailable -> Optional.of(THREADS_UNKNOWN.formatted(unavailable.reason().value()));
            case ReadThreadsView read -> read.threads().isEmpty() ? Optional.of(NO_THREADS) : Optional.empty();
        };
    }

    /** Each file with its status letter, its name, and {@code ◆n} at the right edge when it has open threads. */
    static List<RowLine> fileLines(ReviewPane pane) {
        return pane.rows().files().stream().map(file -> fileLine(file, pane.openThreadsIn(file.path()))).toList();
    }

    private static RowLine fileLine(DiffFileView file, int openThreads) {
        return new RowLine(
                DiffText.letter(file.status()) + LETTER_GAP,
                Style.EMPTY.fg(DiffText.letterColour(file.status())),
                name(file),
                Style.EMPTY,
                openThreads == 0 ? List.of() : openThreadCells(openThreads));
    }

    private static List<RowCell> openThreadCells(int openThreads) {
        Style style = Style.EMPTY.fg(ThreadText.COLOUR);
        return Stream.concat(Stream.of(OPEN_GLYPH), String.valueOf(openThreads).chars().mapToObj(Character::toString))
                .map(glyph -> new RowCell(glyph, style))
                .toList();
    }

    private static String name(DiffFileView file) {
        String name = lastSegment(file.path().value());
        return file.status() instanceof RenamedFile renamed ? RENAMED.formatted(lastSegment(renamed.from().value()), name) : name;
    }

    private static String lastSegment(String path) {
        return path.substring(path.lastIndexOf(PATH_SEPARATOR) + PATH_SEPARATOR.length());
    }

    /** Each thread in diff order: a mark when still open, its file and line and first author, or the file and {@code outdated}; resolved ones dim. */
    static List<RowLine> commentLines(ReviewPane pane) {
        return pane.comments().stream().map(ThreadInDiffView::thread).map(ReviewText::commentLine).toList();
    }

    private static RowLine commentLine(ReviewThreadView thread) {
        return new RowLine(
                thread.open() ? OPEN_MARK : NO_MARK,
                Style.EMPTY.fg(ThreadText.COLOUR),
                where(thread),
                thread.resolved() ? Style.EMPTY.dim() : Style.EMPTY,
                List.of());
    }

    private static String where(ReviewThreadView thread) {
        String file = lastSegment(thread.path().value());
        if (!(thread.position() instanceof PlacedThread placed)) {
            return OUTDATED_ROW.formatted(file, OUTDATED);
        }
        String lines = placed.side() == ThreadSide.LEFT ? OLD_SIDE + ThreadText.lines(placed) : ThreadText.lines(placed);
        return thread.comments().stream()
                .findFirst()
                .map(first -> PLACED.formatted(file, lines, first.author().value()))
                .orElseGet(() -> PLACED_WITHOUT_AUTHOR.formatted(file, lines));
    }
}

package dev.maxenceng.lazyclaude.shell;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.DiffFileView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReadDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffView;
import dev.maxenceng.lazyclaude.review.published.AddedLine;
import dev.maxenceng.lazyclaude.review.published.ContextLine;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.RemovedLine;

/**
 * The rows of a review's main view, built once whenever the diff, the threads or whether resolved threads show change, so that drawing a
 * frame costs what the screen holds, not what the diff does. The diff lines are numbered from 0 in the order they are listed; those
 * numbers, not rows, are where the cursor is.
 */
final class DiffRows {

    private static final int NO_LINES = 0;
    private static final int MIN_NUMBER_WIDTH = 4;
    private static final DiffRows NONE = new DiffRows(List.of(), List.of());

    private final List<DiffFileView> files;
    private final List<DiffRow> rows;
    private final int[] rowOfLine;
    private final int[] fileOfLine;
    private final int[] fileStarts;
    private final int numberWidth;

    private DiffRows(List<DiffFileView> files, List<DiffRow> rows) {
        this.files = files;
        this.rows = rows;
        this.rowOfLine = IntStream.range(0, rows.size()).filter(row -> rows.get(row) instanceof LineRow).toArray();
        this.fileOfLine = Arrays.stream(rowOfLine).map(row -> ((LineRow) rows.get(row)).file()).toArray();
        this.fileStarts = IntStream.range(0, fileOfLine.length)
                .filter(line -> line == 0 || fileOfLine[line] != fileOfLine[line - 1])
                .toArray();
        this.numberWidth = Arrays.stream(rowOfLine)
                .map(row -> digitsOf(((LineRow) rows.get(row)).line()))
                .max()
                .orElse(MIN_NUMBER_WIDTH);
    }

    private static int digitsOf(DiffLine line) {
        int largest = switch (line) {
            case AddedLine added -> added.newNumber().value();
            case RemovedLine removed -> removed.oldNumber().value();
            case ContextLine kept -> Math.max(kept.oldNumber().value(), kept.newNumber().value());
        };
        return Math.max(MIN_NUMBER_WIDTH, String.valueOf(largest).length());
    }

    /** How many digits the longest line number of the diff has, so that the number gutters are as wide as they need to be. */
    int numberWidth() {
        return numberWidth;
    }

    /** The rows of {@code diff} with the threads that show under its lines; none when the diff was not read. */
    static DiffRows of(
            PullRequestDiffView diff,
            ThreadsInDiffView threads,
            PendingCommentsInDiffView pending,
            Optional<CommentBox> box,
            boolean showResolved) {
        if (!(diff instanceof ReadDiffView read)) {
            return NONE;
        }
        List<DiffRow> lineRows = IntStream.range(0, read.files().size())
                .boxed()
                .flatMap(file -> fileRows(file, read.files().get(file)))
                .toList();
        return new DiffRows(read.files(), withThreads(lineRows, inlineThreads(threads, showResolved), inlinePending(pending), box));
    }

    private static Map<Integer, List<ReviewThreadView>> inlineThreads(ThreadsInDiffView threads, boolean showResolved) {
        return threads.threads().stream()
                .filter(placed -> placed.line() instanceof OnDiffLine)
                .filter(placed -> !placed.thread().resolved() || showResolved)
                .collect(Collectors.groupingBy(
                        placed -> ((OnDiffLine) placed.line()).index().value(),
                        Collectors.mapping(ThreadInDiffView::thread, Collectors.toList())));
    }

    private static Map<Integer, List<PendingCommentView>> inlinePending(PendingCommentsInDiffView pending) {
        return pending.comments().stream()
                .filter(placed -> placed.line() instanceof OnDiffLine)
                .collect(Collectors.groupingBy(
                        placed -> ((OnDiffLine) placed.line()).index().value(),
                        Collectors.mapping(PendingInDiffView::comment, Collectors.toList())));
    }

    private static Stream<DiffRow> fileRows(int index, DiffFileView file) {
        return Stream.concat(
                Stream.of(new FileHeaderRow(file)),
                file.hunks().stream()
                        .flatMap(hunk -> Stream.concat(
                                Stream.of(new HunkHeaderRow(hunk.header())),
                                hunk.lines().stream().map(line -> new LineRow(line, index)))));
    }

    /** {@code rows} with each line's threads under it, the lines counted in the order they are listed. */
    private static List<DiffRow> withThreads(
            List<DiffRow> rows,
            Map<Integer, List<ReviewThreadView>> inline,
            Map<Integer, List<PendingCommentView>> pending,
            Optional<CommentBox> box) {
        int[] lineRows = IntStream.range(0, rows.size()).filter(row -> rows.get(row) instanceof LineRow).toArray();
        return IntStream.range(0, rows.size())
                .boxed()
                .flatMap(row -> Stream.concat(
                        Stream.of(rows.get(row)),
                        rows.get(row) instanceof LineRow
                                ? Stream.concat(
                                        inline.getOrDefault(Arrays.binarySearch(lineRows, row), List.of()).stream().map(ThreadRow::new),
                                        Stream.concat(
                                                pending.getOrDefault(Arrays.binarySearch(lineRows, row), List.of()).stream().map(PendingRow::new),
                                                box.filter(open -> open.inlineAt().equals(Optional.of(Arrays.binarySearch(lineRows, row))))
                                                        .stream()
                                                        .map(BoxRow::new)))
                                : Stream.empty()))
                .toList();
    }

    List<DiffFileView> files() {
        return files;
    }

    int size() {
        return rows.size();
    }

    DiffRow row(int index) {
        return rows.get(index);
    }

    /** The row of the box drawn inline, if one is. */
    Optional<Integer> boxRow() {
        return IntStream.range(0, rows.size()).filter(row -> rows.get(row) instanceof BoxRow).boxed().findFirst();
    }

    int lineCount() {
        return rowOfLine.length;
    }

    /** Whether the diff has a line the cursor can stand on. */
    boolean hasLines() {
        return rowOfLine.length != NO_LINES;
    }

    boolean isEmpty() {
        return rows.isEmpty();
    }

    int rowOfLine(int line) {
        return rowOfLine[line];
    }

    /** The diff line the row at {@code row} is, if it is one. */
    Optional<Integer> lineOfRow(int row) {
        int line = Arrays.binarySearch(rowOfLine, row);
        return line >= 0 ? Optional.of(line) : Optional.empty();
    }

    int fileOfLine(int line) {
        return fileOfLine[line];
    }

    /** The first line of the file at {@code file}; none for a file with no lines, as a binary or a plainly renamed one. */
    Optional<Integer> firstLineOf(int file) {
        return Arrays.stream(fileStarts).filter(start -> fileOfLine[start] == file).boxed().findFirst();
    }

    Optional<Integer> firstLineOfNextFile(int line) {
        return Arrays.stream(fileStarts).filter(start -> start > line).boxed().findFirst();
    }

    /** The first line of the file before the one {@code line} is in. */
    Optional<Integer> firstLineOfPreviousFile(int line) {
        int current = firstLineOf(fileOfLine[line]).orElse(0);
        return Arrays.stream(fileStarts).filter(start -> start < current).boxed().reduce((_, later) -> later);
    }
}

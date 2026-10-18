package dev.maxenceng.lazyclaude.shell;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.DiffFileView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.HunkView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReadDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReadThreadsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.review.published.AddedLine;
import dev.maxenceng.lazyclaude.review.published.ContextLine;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.RemovedLine;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;

/**
 * The rows of a review's main view, built once whenever the diff, the threads or whether resolved threads show change, so that drawing a
 * frame costs what the screen holds, not what the diff does. The diff lines are numbered from 0 in the order they are listed; those
 * numbers, not rows, are where the cursor is.
 */
final class DiffRows {

    private static final int MIN_NUMBER_WIDTH = 4;
    private static final DiffRows NONE = new DiffRows(List.of(), List.of());

    private final List<DiffFileView> files;
    private final List<DiffRow> rows;
    private final int[] rowOfLine;
    private final int[] fileOfLine;
    private final int[] fileStarts;
    private final Map<ThreadAnchor, Integer> lineOfAnchor;
    private final int numberWidth;

    private DiffRows(List<DiffFileView> files, List<DiffRow> rows) {
        this.files = files;
        this.rows = rows;
        this.rowOfLine = IntStream.range(0, rows.size()).filter(row -> rows.get(row) instanceof LineRow).toArray();
        this.fileOfLine = Arrays.stream(rowOfLine).map(row -> ((LineRow) rows.get(row)).file()).toArray();
        this.fileStarts = IntStream.range(0, fileOfLine.length)
                .filter(line -> line == 0 || fileOfLine[line] != fileOfLine[line - 1])
                .toArray();
        this.lineOfAnchor = IntStream.range(0, rowOfLine.length)
                .boxed()
                .flatMap(line -> anchorsOf(files.get(fileOfLine[line]), ((LineRow) rows.get(rowOfLine[line])).line())
                        .map(anchor -> Map.entry(anchor, line)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (first, _) -> first));
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

    /** The rows of {@code diff} with the threads that sit on its lines; none when the diff was not read. */
    static DiffRows of(PullRequestDiffView diff, ThreadsView threads, boolean showResolved) {
        if (!(diff instanceof ReadDiffView read)) {
            return NONE;
        }
        Map<ThreadAnchor, List<ReviewThreadView>> inline = inlineThreads(threads, showResolved);
        return new DiffRows(
                read.files(),
                IntStream.range(0, read.files().size())
                        .boxed()
                        .flatMap(file -> fileRows(file, read.files().get(file), inline))
                        .toList());
    }

    private static Map<ThreadAnchor, List<ReviewThreadView>> inlineThreads(ThreadsView threads, boolean showResolved) {
        if (!(threads instanceof ReadThreadsView read)) {
            return Map.of();
        }
        return read.threads().stream()
                .filter(thread -> thread.position() instanceof PlacedThread)
                .filter(thread -> !thread.resolved() || showResolved)
                .collect(Collectors.groupingBy(DiffRows::anchorOf));
    }

    private static ThreadAnchor anchorOf(ReviewThreadView thread) {
        PlacedThread placed = (PlacedThread) thread.position();
        return new ThreadAnchor(thread.path(), placed.side(), placed.last());
    }

    private static Stream<DiffRow> fileRows(int index, DiffFileView file, Map<ThreadAnchor, List<ReviewThreadView>> inline) {
        return Stream.concat(
                Stream.of(new FileHeaderRow(file)),
                file.hunks().stream().flatMap(hunk -> hunkRows(index, file, hunk, inline)));
    }

    private static Stream<DiffRow> hunkRows(int index, DiffFileView file, HunkView hunk, Map<ThreadAnchor, List<ReviewThreadView>> inline) {
        return Stream.concat(
                Stream.of(new HunkHeaderRow(hunk.header())),
                hunk.lines().stream()
                        .flatMap(line -> Stream.concat(
                                Stream.of(new LineRow(line, index)),
                                anchorsOf(file, line)
                                        .flatMap(anchor -> inline.getOrDefault(anchor, List.of()).stream())
                                        .map(ThreadRow::new))));
    }

    /** The places a thread can hang on a line: its new number on the right, its old number on the left, a context line having both. */
    private static Stream<ThreadAnchor> anchorsOf(DiffFileView file, DiffLine line) {
        return switch (line) {
            case AddedLine added -> Stream.of(new ThreadAnchor(file.path(), ThreadSide.RIGHT, added.newNumber()));
            case RemovedLine removed -> Stream.of(new ThreadAnchor(file.path(), ThreadSide.LEFT, removed.oldNumber()));
            case ContextLine kept ->
                Stream.of(
                        new ThreadAnchor(file.path(), ThreadSide.RIGHT, kept.newNumber()),
                        new ThreadAnchor(file.path(), ThreadSide.LEFT, kept.oldNumber()));
        };
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

    int lineCount() {
        return rowOfLine.length;
    }

    int rowOfLine(int line) {
        return rowOfLine[line];
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

    /** The line the thread shows under; none for an outdated thread, or one on a line the diff does not hold. */
    Optional<Integer> lineOf(ReviewThreadView thread) {
        return thread.position() instanceof PlacedThread
                ? Optional.ofNullable(lineOfAnchor.get(anchorOf(thread)))
                : Optional.empty();
    }
}

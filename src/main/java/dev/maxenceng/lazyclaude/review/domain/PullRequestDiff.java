package dev.maxenceng.lazyclaude.review.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.AddedLine;
import dev.maxenceng.lazyclaude.review.published.ContextLine;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.DiffLineNumber;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.NoDiffLine;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.RemovedLine;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;

/**
 * The unified diff of a pull request, every file in one scroll. It owns where a thread meets a diff line: a {@code RIGHT} thread on the
 * line of its new number, a {@code LEFT} one on the line of its old number, a context line holding both.
 */
public record PullRequestDiff(List<DiffFile> files) {

    private static final Comparator<ThreadInDiff> IN_DIFF_ORDER = Comparator.comparing((ThreadInDiff placed) -> placed.line() instanceof NoDiffLine)
            .thenComparingInt(PullRequestDiff::indexOf);
    private static final Comparator<PendingInDiff> PENDING_IN_DIFF_ORDER = Comparator.comparing((PendingInDiff placed) -> placed.line() instanceof NoDiffLine)
            .thenComparingInt(placed -> indexOf(placed.line()));

    public PullRequestDiff {
        Assert.notNull("files", files);
    }

    /** Each of {@code threads} with the diff line it shows under, in diff order; one on a line this diff does not hold shows under none. */
    public ThreadsInDiff place(ReadThreads threads) {
        Map<LineAnchor, DiffLineIndex> lineOfAnchor = lineOfAnchor();
        return new ThreadsInDiff(threads.byFileAndLine().stream()
                .map(thread -> ThreadInDiff.builder().thread(thread).line(lineOf(thread, lineOfAnchor)).build())
                .sorted(IN_DIFF_ORDER)
                .toList());
    }

    /** Each of the pending {@code comments} with the diff line it shows under, in diff order; one on a line this diff does not hold shows under none. */
    public PendingCommentsInDiff place(List<PendingComment> comments) {
        Map<LineAnchor, DiffLineIndex> lineOfAnchor = lineOfAnchor();
        return new PendingCommentsInDiff(comments.stream()
                .map(comment -> PendingInDiff.builder()
                        .comment(comment)
                        .line(lineOf(comment.anchor().path(), comment.anchor().position(), lineOfAnchor))
                        .build())
                .sorted(PENDING_IN_DIFF_ORDER)
                .toList());
    }

    /** The comment on the line at {@code first} or on the range from it to {@code last}, in whichever order they come. */
    public PendingOnDiff commentOn(DiffLineIndex first, DiffLineIndex last) {
        int from = Math.min(first.value(), last.value());
        int to = Math.max(first.value(), last.value());
        if (to >= lineCount()) {
            throw new DiffLineMissingException();
        }
        HunkInDiff hunk = hunks().stream()
                .filter(candidate -> candidate.holds(from))
                .findFirst()
                .orElseThrow(DiffLineMissingException::new);
        if (!hunk.holds(to)) {
            throw new RangeAcrossHunksException();
        }
        List<DiffLine> range = hunk.linesFrom(from, to);
        ThreadSide side = sideOf(range);
        return PendingOnDiff.builder()
                .path(hunk.file().path())
                .position(PlacedThread.builder()
                        .side(side)
                        .first(numberOn(side, range.getFirst()).orElseThrow(RangeAcrossSidesException::new))
                        .last(numberOn(side, range.getLast()).orElseThrow(RangeAcrossSidesException::new))
                        .build())
                .build();
    }

    private int lineCount() {
        return files.stream().flatMap(file -> file.hunks().stream()).mapToInt(hunk -> hunk.lines().size()).sum();
    }

    private List<HunkInDiff> hunks() {
        List<Map.Entry<DiffFile, Hunk>> hunks =
                files.stream().flatMap(file -> file.hunks().stream().map(hunk -> Map.entry(file, hunk))).toList();
        return IntStream.range(0, hunks.size())
                .mapToObj(index -> HunkInDiff.builder()
                        .file(hunks.get(index).getKey())
                        .hunk(hunks.get(index).getValue())
                        .start(new DiffLineIndex(hunks.subList(0, index).stream()
                                .mapToInt(before -> before.getValue().lines().size())
                                .sum()))
                        .build())
                .toList();
    }

    /**
     * A range holding a removed line is on the old side, and one holding an added line, or only unchanged ones, on the new; one holding both
     * has no side numbering all of it.
     */
    private static ThreadSide sideOf(List<DiffLine> range) {
        boolean removes = range.stream().anyMatch(RemovedLine.class::isInstance);
        if (removes && range.stream().anyMatch(AddedLine.class::isInstance)) {
            throw new RangeAcrossSidesException();
        }
        return removes ? ThreadSide.LEFT : ThreadSide.RIGHT;
    }

    private static Optional<DiffLineNumber> numberOn(ThreadSide side, DiffLine line) {
        return switch (line) {
            case AddedLine added -> side == ThreadSide.RIGHT ? Optional.of(added.newNumber()) : Optional.empty();
            case RemovedLine removed -> side == ThreadSide.LEFT ? Optional.of(removed.oldNumber()) : Optional.empty();
            case ContextLine kept -> Optional.of(side == ThreadSide.RIGHT ? kept.newNumber() : kept.oldNumber());
        };
    }

    private static int indexOf(ThreadInDiff placed) {
        return indexOf(placed.line());
    }

    private static int indexOf(ThreadLine line) {
        return line instanceof OnDiffLine(DiffLineIndex index) ? index.value() : 0;
    }

    private static ThreadLine lineOf(ReviewThread thread, Map<LineAnchor, DiffLineIndex> lineOfAnchor) {
        return lineOf(thread.path(), thread.position(), lineOfAnchor);
    }

    private static ThreadLine lineOf(DiffPath path, ThreadPosition position, Map<LineAnchor, DiffLineIndex> lineOfAnchor) {
        if (!(position instanceof PlacedThread(ThreadSide side, _, DiffLineNumber last))) {
            return new NoDiffLine();
        }
        return Optional.ofNullable(lineOfAnchor.get(LineAnchor.builder().path(path).side(side).number(last).build()))
                .<ThreadLine>map(OnDiffLine::new)
                .orElseGet(NoDiffLine::new);
    }

    private Map<LineAnchor, DiffLineIndex> lineOfAnchor() {
        List<Map.Entry<DiffFile, DiffLine>> lines = files.stream()
                .flatMap(file -> file.hunks().stream().flatMap(hunk -> hunk.lines().stream()).map(line -> Map.entry(file, line)))
                .toList();
        return IntStream.range(0, lines.size())
                .boxed()
                .flatMap(index -> anchorsOf(lines.get(index).getKey(), lines.get(index).getValue())
                        .map(anchor -> Map.entry(anchor, new DiffLineIndex(index))))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (first, _) -> first));
    }

    private static Stream<LineAnchor> anchorsOf(DiffFile file, DiffLine line) {
        return switch (line) {
            case AddedLine added -> Stream.of(anchor(file, ThreadSide.RIGHT, added.newNumber()));
            case RemovedLine removed -> Stream.of(anchor(file, ThreadSide.LEFT, removed.oldNumber()));
            case ContextLine kept ->
                Stream.of(
                        anchor(file, ThreadSide.RIGHT, kept.newNumber()),
                        anchor(file, ThreadSide.LEFT, kept.oldNumber()));
        };
    }

    private static LineAnchor anchor(DiffFile file, ThreadSide side, DiffLineNumber number) {
        return LineAnchor.builder().path(file.path()).side(side).number(number).build();
    }
}

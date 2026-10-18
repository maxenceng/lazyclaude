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
import dev.maxenceng.lazyclaude.review.published.NoDiffLine;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.RemovedLine;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;

/**
 * The unified diff of a pull request, every file in one scroll. It owns where a thread meets a diff line: a {@code RIGHT} thread on the
 * line of its new number, a {@code LEFT} one on the line of its old number, a context line holding both.
 */
public record PullRequestDiff(List<DiffFile> files) {

    private static final Comparator<ThreadInDiff> IN_DIFF_ORDER = Comparator.comparing((ThreadInDiff placed) -> placed.line() instanceof NoDiffLine)
            .thenComparingInt(PullRequestDiff::indexOf);

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

    private static int indexOf(ThreadInDiff placed) {
        return placed.line() instanceof OnDiffLine on ? on.index().value() : 0;
    }

    private static ThreadLine lineOf(ReviewThread thread, Map<LineAnchor, DiffLineIndex> lineOfAnchor) {
        if (!(thread.position() instanceof PlacedThread placed)) {
            return new NoDiffLine();
        }
        return Optional.ofNullable(lineOfAnchor.get(LineAnchor.builder().path(thread.path()).side(placed.side()).number(placed.last()).build()))
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

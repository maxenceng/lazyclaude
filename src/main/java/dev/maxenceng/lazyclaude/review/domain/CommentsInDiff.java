package dev.maxenceng.lazyclaude.review.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;

/**
 * What the review screen shows of a pull request's comments, from the threads and the pending comments with their lines: which threads
 * show under their lines, what Comments lists and in what order, and which pending comment a line's keys act on.
 */
public record CommentsInDiff(ThreadsInDiff threads, PendingCommentsInDiff pending) {

    public CommentsInDiff {
        Assert.notNull("threads", threads);
        Assert.notNull("pending", pending);
    }

    /** The threads that show under their diff lines: a resolved one only while resolved threads are shown. */
    public List<ThreadInDiff> underLines(boolean resolvedShown) {
        return threads.threads().stream().filter(placed -> resolvedShown || !placed.thread().resolved()).toList();
    }

    /** What Comments lists: your pending comments first, in diff order, then every thread in diff order, resolved ones included. */
    public List<CommentInDiff> listed() {
        return Stream.<CommentInDiff>concat(pending.comments().stream(), threads.threads().stream()).toList();
    }

    /** Per diff line that has pending comments, in diff order, the one written last: the comment {@code enter} edits and {@code x} deletes there. */
    public List<PendingInDiff> lastOnLines() {
        Map<DiffLineIndex, PendingInDiff> last = pending.comments().stream()
                .flatMap(placed -> placed.line() instanceof OnDiffLine(DiffLineIndex index) ? Stream.of(Map.entry(index, placed)) : Stream.empty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, later) -> later, LinkedHashMap::new));
        return last.values().stream().toList();
    }

    public static CommentsInDiffBuilder builder() {
        return new CommentsInDiffBuilder();
    }

    public static final class CommentsInDiffBuilder {
        private ThreadsInDiff threads;
        private PendingCommentsInDiff pending;

        private CommentsInDiffBuilder() {}

        public CommentsInDiffBuilder threads(ThreadsInDiff threads) {
            this.threads = threads;
            return this;
        }

        public CommentsInDiffBuilder pending(PendingCommentsInDiff pending) {
            this.pending = pending;
            return this;
        }

        public CommentsInDiff build() {
            return new CommentsInDiff(threads, pending);
        }
    }
}

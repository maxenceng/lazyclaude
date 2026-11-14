package dev.maxenceng.lazyclaude.review.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/**
 * What the review screen shows of a pull request's comments, from the threads and the pending comments with their lines: which threads
 * show and where, what Comments lists and in what order, and which pending comment the keys act on at each place.
 */
public record CommentsInDiff(ThreadsInDiff threads, PendingCommentsInDiff pending) {

    public CommentsInDiff {
        Assert.notNull("threads", threads);
        Assert.notNull("pending", pending);
    }

    /** The threads that show, each where it shows: a resolved one only while resolved threads are shown. */
    public List<ThreadInDiff> shown(boolean resolvedShown) {
        return threads.threads().stream().filter(placed -> resolvedShown || !placed.thread().resolved()).toList();
    }

    /** What Comments lists: your pending comments first, in diff order, then every thread in diff order, resolved ones included. */
    public List<CommentInDiff> listed() {
        return Stream.<CommentInDiff>concat(pending.comments().stream(), threads.threads().stream()).toList();
    }

    /**
     * Per place that has pending comments — a diff line, a file's header, or after the diff — in diff order, the one written last: the
     * comment {@code enter} edits and {@code x} deletes there.
     */
    public List<PendingInDiff> lastAtPlaces() {
        Map<ThreadLine, PendingInDiff> last = pending.comments().stream()
                .collect(Collectors.toMap(PendingInDiff::line, placed -> placed, (_, later) -> later, LinkedHashMap::new));
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

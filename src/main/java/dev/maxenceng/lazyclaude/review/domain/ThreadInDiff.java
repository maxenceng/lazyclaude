package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/** A thread and the diff line it shows under. */
public record ThreadInDiff(ReviewThread thread, ThreadLine line) {

    public ThreadInDiff {
        Assert.notNull("thread", thread);
        Assert.notNull("line", line);
    }

    public static ThreadInDiffBuilder builder() {
        return new ThreadInDiffBuilder();
    }

    public static final class ThreadInDiffBuilder {
        private ReviewThread thread;
        private ThreadLine line;

        private ThreadInDiffBuilder() {}

        public ThreadInDiffBuilder thread(ReviewThread thread) {
            this.thread = thread;
            return this;
        }

        public ThreadInDiffBuilder line(ThreadLine line) {
            this.line = line;
            return this;
        }

        public ThreadInDiff build() {
            return new ThreadInDiff(thread, line);
        }
    }
}

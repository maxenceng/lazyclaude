package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.ThreadInDiff;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/** A thread and the diff line it shows under. */
public record ThreadInDiffView(ReviewThreadView thread, ThreadLine line) {

    static ThreadInDiffView from(ThreadInDiff placed) {
        return builder().thread(ReviewThreadView.from(placed.thread())).line(placed.line()).build();
    }

    public static ThreadInDiffViewBuilder builder() {
        return new ThreadInDiffViewBuilder();
    }

    public static final class ThreadInDiffViewBuilder {
        private ReviewThreadView thread;
        private ThreadLine line;

        private ThreadInDiffViewBuilder() {}

        public ThreadInDiffViewBuilder thread(ReviewThreadView thread) {
            this.thread = thread;
            return this;
        }

        public ThreadInDiffViewBuilder line(ThreadLine line) {
            this.line = line;
            return this;
        }

        public ThreadInDiffView build() {
            return new ThreadInDiffView(thread, line);
        }
    }
}

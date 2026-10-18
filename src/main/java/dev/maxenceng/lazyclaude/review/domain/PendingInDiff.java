package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/** A pending comment and the diff line it shows under. */
public record PendingInDiff(PendingComment comment, ThreadLine line) {

    public PendingInDiff {
        Assert.notNull("comment", comment);
        Assert.notNull("line", line);
    }

    public static PendingInDiffBuilder builder() {
        return new PendingInDiffBuilder();
    }

    public static final class PendingInDiffBuilder {
        private PendingComment comment;
        private ThreadLine line;

        private PendingInDiffBuilder() {}

        public PendingInDiffBuilder comment(PendingComment comment) {
            this.comment = comment;
            return this;
        }

        public PendingInDiffBuilder line(ThreadLine line) {
            this.line = line;
            return this;
        }

        public PendingInDiff build() {
            return new PendingInDiff(comment, line);
        }
    }
}

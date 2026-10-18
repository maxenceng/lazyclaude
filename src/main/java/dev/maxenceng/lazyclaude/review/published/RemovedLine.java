package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A line the pull request removes, numbered on the old side. */
public record RemovedLine(DiffLineContent content, DiffLineNumber oldNumber) implements DiffLine {

    public RemovedLine {
        Assert.notNull("content", content);
        Assert.notNull("old number", oldNumber);
    }

    public static RemovedLineBuilder builder() {
        return new RemovedLineBuilder();
    }

    public static final class RemovedLineBuilder {
        private DiffLineContent content;
        private DiffLineNumber oldNumber;

        private RemovedLineBuilder() {}

        public RemovedLineBuilder content(DiffLineContent content) {
            this.content = content;
            return this;
        }

        public RemovedLineBuilder oldNumber(DiffLineNumber oldNumber) {
            this.oldNumber = oldNumber;
            return this;
        }

        public RemovedLine build() {
            return new RemovedLine(content, oldNumber);
        }
    }
}

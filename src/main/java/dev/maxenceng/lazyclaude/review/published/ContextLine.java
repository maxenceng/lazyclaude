package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A line the pull request leaves as it was, numbered on both sides. */
public record ContextLine(DiffLineContent content, DiffLineNumber oldNumber, DiffLineNumber newNumber) implements DiffLine {

    public ContextLine {
        Assert.notNull("content", content);
        Assert.notNull("old number", oldNumber);
        Assert.notNull("new number", newNumber);
    }

    public static ContextLineBuilder builder() {
        return new ContextLineBuilder();
    }

    public static final class ContextLineBuilder {
        private DiffLineContent content;
        private DiffLineNumber oldNumber;
        private DiffLineNumber newNumber;

        private ContextLineBuilder() {}

        public ContextLineBuilder content(DiffLineContent content) {
            this.content = content;
            return this;
        }

        public ContextLineBuilder oldNumber(DiffLineNumber oldNumber) {
            this.oldNumber = oldNumber;
            return this;
        }

        public ContextLineBuilder newNumber(DiffLineNumber newNumber) {
            this.newNumber = newNumber;
            return this;
        }

        public ContextLine build() {
            return new ContextLine(content, oldNumber, newNumber);
        }
    }
}

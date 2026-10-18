package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A line the pull request adds, numbered on the new side. */
public record AddedLine(DiffLineContent content, DiffLineNumber newNumber) implements DiffLine {

    public AddedLine {
        Assert.notNull("content", content);
        Assert.notNull("new number", newNumber);
    }

    public static AddedLineBuilder builder() {
        return new AddedLineBuilder();
    }

    public static final class AddedLineBuilder {
        private DiffLineContent content;
        private DiffLineNumber newNumber;

        private AddedLineBuilder() {}

        public AddedLineBuilder content(DiffLineContent content) {
            this.content = content;
            return this;
        }

        public AddedLineBuilder newNumber(DiffLineNumber newNumber) {
            this.newNumber = newNumber;
            return this;
        }

        public AddedLine build() {
            return new AddedLine(content, newNumber);
        }
    }
}

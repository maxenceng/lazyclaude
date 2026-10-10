package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.FrontmatterKey;
import dev.maxenceng.lazyclaude.board.published.FrontmatterValue;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One key of a ticket's frontmatter with the text it shows. */
public record FrontmatterEntry(FrontmatterKey key, FrontmatterValue value) {

    public FrontmatterEntry {
        Assert.notNull("key", key);
        Assert.notNull("value", value);
    }

    public static FrontmatterEntryBuilder builder() {
        return new FrontmatterEntryBuilder();
    }

    public static final class FrontmatterEntryBuilder {
        private FrontmatterKey key;
        private FrontmatterValue value;

        private FrontmatterEntryBuilder() {}

        public FrontmatterEntryBuilder key(FrontmatterKey key) {
            this.key = key;
            return this;
        }

        public FrontmatterEntryBuilder value(FrontmatterValue value) {
            this.value = value;
            return this;
        }

        public FrontmatterEntry build() {
            return new FrontmatterEntry(key, value);
        }
    }
}

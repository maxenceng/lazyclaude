package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.FrontmatterEntry;
import dev.maxenceng.lazyclaude.board.published.FrontmatterKey;
import dev.maxenceng.lazyclaude.board.published.FrontmatterValue;

/** One key of a ticket's frontmatter and the text it shows. */
public record FrontmatterEntryView(FrontmatterKey key, FrontmatterValue value) {

    static FrontmatterEntryView from(FrontmatterEntry entry) {
        return builder().key(entry.key()).value(entry.value()).build();
    }

    public static FrontmatterEntryViewBuilder builder() {
        return new FrontmatterEntryViewBuilder();
    }

    public static final class FrontmatterEntryViewBuilder {
        private FrontmatterKey key;
        private FrontmatterValue value;

        private FrontmatterEntryViewBuilder() {}

        public FrontmatterEntryViewBuilder key(FrontmatterKey key) {
            this.key = key;
            return this;
        }

        public FrontmatterEntryViewBuilder value(FrontmatterValue value) {
            this.value = value;
            return this;
        }

        public FrontmatterEntryView build() {
            return new FrontmatterEntryView(key, value);
        }
    }
}

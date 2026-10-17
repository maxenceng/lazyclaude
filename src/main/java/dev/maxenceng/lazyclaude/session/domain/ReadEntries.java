package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.TranscriptEntry;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;

/**
 * The entries written between {@code start} and {@code next}. {@code start} is where this read began, which is not the offset
 * asked for when the transcript is shorter than that: it was replaced, and what was shown before no longer holds.
 */
public record ReadEntries(List<TranscriptEntry> entries, TranscriptOffset start, TranscriptOffset next)
        implements TranscriptRead {

    public ReadEntries {
        Assert.notNull("entries", entries);
        Assert.notNull("start", start);
        Assert.notNull("next", next);
    }

    public static ReadEntriesBuilder builder() {
        return new ReadEntriesBuilder();
    }

    public static final class ReadEntriesBuilder {
        private List<TranscriptEntry> entries = List.of();
        private TranscriptOffset start;
        private TranscriptOffset next;

        private ReadEntriesBuilder() {}

        public ReadEntriesBuilder entries(List<TranscriptEntry> entries) {
            this.entries = entries;
            return this;
        }

        public ReadEntriesBuilder start(TranscriptOffset start) {
            this.start = start;
            return this;
        }

        public ReadEntriesBuilder next(TranscriptOffset next) {
            this.next = next;
            return this;
        }

        public ReadEntries build() {
            return new ReadEntries(entries, start, next);
        }
    }
}

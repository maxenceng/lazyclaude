package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.ReadEntries;
import dev.maxenceng.lazyclaude.session.published.TranscriptEntry;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;

/**
 * The entries a transcript gained between {@code start} and {@code next}. They follow what the shell showed when {@code start} is the
 * offset it asked from; when it is not, the transcript was replaced and these are all of it.
 */
public record ReadTranscriptView(List<TranscriptEntry> entries, TranscriptOffset start, TranscriptOffset next)
        implements TranscriptView {

    static ReadTranscriptView from(ReadEntries read) {
        return builder().entries(read.entries()).start(read.start()).next(read.next()).build();
    }

    public static ReadTranscriptViewBuilder builder() {
        return new ReadTranscriptViewBuilder();
    }

    public static final class ReadTranscriptViewBuilder {
        private List<TranscriptEntry> entries = List.of();
        private TranscriptOffset start;
        private TranscriptOffset next;

        private ReadTranscriptViewBuilder() {}

        public ReadTranscriptViewBuilder entries(List<TranscriptEntry> entries) {
            this.entries = entries;
            return this;
        }

        public ReadTranscriptViewBuilder start(TranscriptOffset start) {
            this.start = start;
            return this;
        }

        public ReadTranscriptViewBuilder next(TranscriptOffset next) {
            this.next = next;
            return this;
        }

        public ReadTranscriptView build() {
            return new ReadTranscriptView(entries, start, next);
        }
    }
}

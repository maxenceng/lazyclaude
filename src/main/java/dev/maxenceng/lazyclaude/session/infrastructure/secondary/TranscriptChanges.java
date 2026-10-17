package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** What one drain of the watch saw: the transcripts that changed, or that every one must be re-stated, after an overflow or a new folder. */
record TranscriptChanges(boolean everything, List<Path> files) {

    static TranscriptChanges none() {
        return new TranscriptChanges(false, List.of());
    }

    static TranscriptChanges everythingChanged() {
        return new TranscriptChanges(true, List.of());
    }

    static TranscriptChanges of(List<Path> files) {
        return new TranscriptChanges(false, files);
    }

    TranscriptChanges and(TranscriptChanges other) {
        return new TranscriptChanges(
                everything || other.everything, Stream.concat(files.stream(), other.files.stream()).toList());
    }
}

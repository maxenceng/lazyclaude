package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** What one drain of the watch saw: the transcripts that changed, or that the watch overflowed and every one must be re-stated. */
record TranscriptChanges(boolean overflowed, List<Path> files) {

    static TranscriptChanges none() {
        return new TranscriptChanges(false, List.of());
    }

    static TranscriptChanges overflow() {
        return new TranscriptChanges(true, List.of());
    }

    static TranscriptChanges of(List<Path> files) {
        return new TranscriptChanges(false, files);
    }

    TranscriptChanges and(TranscriptChanges other) {
        return new TranscriptChanges(
                overflowed || other.overflowed, Stream.concat(files.stream(), other.files.stream()).toList());
    }
}

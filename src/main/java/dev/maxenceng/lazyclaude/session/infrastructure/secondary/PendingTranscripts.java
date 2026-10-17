package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The transcripts changed and not read since, each with when it last changed; one is read once it has been quiet long enough. */
record PendingTranscripts(Map<Path, Instant> lastChanged) {

    static PendingTranscripts none() {
        return new PendingTranscripts(Map.of());
    }

    PendingTranscripts changed(Collection<Path> files, Instant at) {
        return new PendingTranscripts(Stream.concat(
                        lastChanged.entrySet().stream().filter(pending -> !files.contains(pending.getKey())),
                        files.stream().distinct().map(file -> Map.entry(file, at)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /** The transcripts that have not changed since {@code quietSince}. */
    List<Path> quietSince(Instant quietSince) {
        return lastChanged.entrySet().stream()
                .filter(pending -> !pending.getValue().isAfter(quietSince))
                .map(Map.Entry::getKey)
                .toList();
    }

    PendingTranscripts without(Collection<Path> files) {
        return new PendingTranscripts(lastChanged.entrySet().stream()
                .filter(pending -> !files.contains(pending.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The transcripts a scan found, and the folders it could not list, whose transcripts are therefore not known to be gone. */
record TranscriptListing(Set<Path> present, Set<Path> unlisted) {

    static TranscriptListing none() {
        return new TranscriptListing(Set.of(), Set.of());
    }

    static TranscriptListing found(Set<Path> present) {
        return new TranscriptListing(present, Set.of());
    }

    static TranscriptListing unlistable(Path folder) {
        return new TranscriptListing(Set.of(), Set.of(folder));
    }

    TranscriptListing and(TranscriptListing other) {
        return new TranscriptListing(union(present, other.present), union(unlisted, other.unlisted));
    }

    /** Whether {@code file} was not found, though its folder and the root above it were both listed. */
    boolean isGone(Path file) {
        return !present.contains(file)
                && !unlisted.contains(file.getParent())
                && !unlisted.contains(file.getParent().getParent());
    }

    private static Set<Path> union(Set<Path> some, Set<Path> others) {
        return Stream.concat(some.stream(), others.stream()).collect(Collectors.toUnmodifiableSet());
    }
}

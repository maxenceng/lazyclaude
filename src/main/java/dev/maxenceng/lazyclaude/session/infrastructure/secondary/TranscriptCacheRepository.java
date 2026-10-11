package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads and writes the cache of what the transcripts said. A cache that is missing, does not parse or was written in
 * another format version is empty, not a failure: it is rewritten by the next write. A write goes whole to a temporary file
 * beside the cache and is renamed over it, so a reader never meets half a cache.
 */
public final class TranscriptCacheRepository {

    static final int FORMAT_VERSION = 1;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String TEMPORARY_SUFFIX = ".tmp";

    private final Path file;

    public TranscriptCacheRepository(Path file) {
        this.file = file;
    }

    List<CachedTranscript> load() {
        try {
            TranscriptCache cache = MAPPER.readValue(Files.readAllBytes(file), TranscriptCache.class);
            if (cache == null || cache.version() == null || cache.version() != FORMAT_VERSION || cache.transcripts() == null) {
                return List.of();
            }
            if (!cache.transcripts().stream().allMatch(transcript -> transcript != null && transcript.isWhole())) {
                return List.of();
            }
            return cache.transcripts();
        } catch (IOException | JacksonException unusable) {
            return List.of();
        }
    }

    /** @throws IOException when the cache cannot be written, with the reason in its message */
    void write(Collection<CachedTranscript> transcripts) throws IOException {
        byte[] json;
        try {
            json = MAPPER.writeValueAsBytes(new TranscriptCache(FORMAT_VERSION, List.copyOf(transcripts)));
        } catch (JacksonException e) {
            throw new IOException("cannot serialise the cache: " + e.getOriginalMessage(), e);
        }
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + TEMPORARY_SUFFIX);
        Files.write(temporary, json);
        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    Path file() {
        return file;
    }
}

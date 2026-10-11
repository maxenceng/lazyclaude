package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptCacheFixture.cachedTranscript;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptCacheFixture.otherCachedTranscript;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TranscriptCacheRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void shouldReadWhatItWrote() throws IOException {
        TranscriptCacheRepository cache = new TranscriptCacheRepository(directory.resolve("lazyclaude/past-sessions.json"));

        cache.write(List.of(cachedTranscript(), otherCachedTranscript()));

        assertThat(cache.load()).containsExactlyInAnyOrder(cachedTranscript(), otherCachedTranscript());
    }

    @Test
    void shouldBeEmptyWhenTheCacheIsMissing() {
        assertThat(new TranscriptCacheRepository(directory.resolve("missing.json")).load()).isEmpty();
    }

    @Test
    void shouldBeEmptyWhenTheCacheDoesNotParse() throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheRepository(file).load()).isEmpty();
    }

    @Test
    void shouldBeEmptyWhenTheCacheIsOfAnotherFormatVersion() throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(file, "{\"version\":%d,\"transcripts\":[]}".formatted(TranscriptCacheRepository.FORMAT_VERSION + 1), StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheRepository(file).load())
                .as("a field this version does not know cannot be trusted, so the cache is read from scratch")
                .isEmpty();
    }

    @Test
    void shouldBeEmptyWhenTheCacheCarriesNoVersion() throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(file, "{\"transcripts\":[]}", StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheRepository(file).load()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[null]", "[{\"size\":1}]", "[{\"path\":\"x.jsonl\",\"fileKey\":\"k\",\"sessionId\":\"s\"}]"})
    void shouldBeEmptyWhenAnEntryOfTheCacheIsNotWhole(String transcripts) throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(
                file,
                "{\"version\":%d,\"transcripts\":%s}".formatted(TranscriptCacheRepository.FORMAT_VERSION, transcripts),
                StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheRepository(file).load())
                .as("an entry with no path or file key would fail every scan and every read of the panel, so the cache is read from scratch")
                .isEmpty();
    }

    @Test
    void shouldReplaceAnOlderCacheWholeAndLeaveNoTemporaryFileBehind() throws IOException {
        Path file = directory.resolve("lazyclaude/past-sessions.json");
        TranscriptCacheRepository cache = new TranscriptCacheRepository(file);
        cache.write(List.of(cachedTranscript(), otherCachedTranscript()));

        cache.write(List.of(otherCachedTranscript()));

        assertThat(cache.load()).containsExactly(otherCachedTranscript());
        try (Stream<Path> files = Files.list(file.getParent())) {
            assertThat(files.map(entry -> entry.getFileName().toString()))
                    .as("the temporary file is renamed over the cache, never left beside it")
                    .containsExactly("past-sessions.json");
        }
    }

    @Test
    void shouldFailToWriteWhenItsFolderCannotBeMade() throws IOException {
        Path blocker = directory.resolve("blocker");
        Files.writeString(blocker, "a file where the cache folder should be", StandardCharsets.UTF_8);
        TranscriptCacheRepository cache = new TranscriptCacheRepository(blocker.resolve("lazyclaude/past-sessions.json"));

        assertThatThrownBy(() -> cache.write(List.of(cachedTranscript()))).isInstanceOf(IOException.class);
    }
}

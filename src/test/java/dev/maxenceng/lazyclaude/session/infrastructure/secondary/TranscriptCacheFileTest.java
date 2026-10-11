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

class TranscriptCacheFileTest {

    @TempDir
    Path directory;

    @Test
    void shouldReadWhatItWrote() throws IOException {
        TranscriptCacheFile cache = new TranscriptCacheFile(directory.resolve("lazyclaude/past-sessions.json"));

        cache.write(List.of(cachedTranscript(), otherCachedTranscript()));

        assertThat(cache.load()).containsExactlyInAnyOrder(cachedTranscript(), otherCachedTranscript());
    }

    @Test
    void shouldBeEmptyWhenTheCacheIsMissing() {
        assertThat(new TranscriptCacheFile(directory.resolve("missing.json")).load()).isEmpty();
    }

    @Test
    void shouldBeEmptyWhenTheCacheDoesNotParse() throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheFile(file).load()).isEmpty();
    }

    @Test
    void shouldBeEmptyWhenTheCacheIsOfAnotherFormatVersion() throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(file, "{\"version\":%d,\"transcripts\":[]}".formatted(TranscriptCacheFile.FORMAT_VERSION + 1), StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheFile(file).load())
                .as("a field this version does not know cannot be trusted, so the cache is read from scratch")
                .isEmpty();
    }

    @Test
    void shouldBeEmptyWhenTheCacheCarriesNoVersion() throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(file, "{\"transcripts\":[]}", StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheFile(file).load()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[null]", "[{\"size\":1}]", "[{\"path\":\"x.jsonl\",\"fileKey\":\"k\",\"sessionId\":\"s\"}]"})
    void shouldBeEmptyWhenAnEntryOfTheCacheIsNotWhole(String transcripts) throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.writeString(
                file,
                "{\"version\":%d,\"transcripts\":%s}".formatted(TranscriptCacheFile.FORMAT_VERSION, transcripts),
                StandardCharsets.UTF_8);

        assertThat(new TranscriptCacheFile(file).load())
                .as("an entry with no path or file key would fail every scan and every read of the panel, so the cache is read from scratch")
                .isEmpty();
    }

    @Test
    void shouldReplaceAnOlderCacheWholeAndLeaveNoTemporaryFileBehind() throws IOException {
        Path file = directory.resolve("lazyclaude/past-sessions.json");
        TranscriptCacheFile cache = new TranscriptCacheFile(file);
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
    void shouldLeaveNoTemporaryFileBehindWhenTheRenameFails() throws IOException {
        Path file = directory.resolve("past-sessions.json");
        Files.createDirectories(file);
        Files.writeString(file.resolve("entry"), "a folder where the cache should be", StandardCharsets.UTF_8);
        TranscriptCacheFile cache = new TranscriptCacheFile(file);

        assertThatThrownBy(() -> cache.write(List.of(cachedTranscript()))).isInstanceOf(IOException.class);
        try (Stream<Path> files = Files.list(directory)) {
            assertThat(files.map(entry -> entry.getFileName().toString()))
                    .as("a rename that failed must not leave its temporary file beside the cache")
                    .containsExactly("past-sessions.json");
        }
    }

    @Test
    void shouldFailToWriteWhenItsFolderCannotBeMade() throws IOException {
        Path blocker = directory.resolve("blocker");
        Files.writeString(blocker, "a file where the cache folder should be", StandardCharsets.UTF_8);
        TranscriptCacheFile cache = new TranscriptCacheFile(blocker.resolve("lazyclaude/past-sessions.json"));

        assertThatThrownBy(() -> cache.write(List.of(cachedTranscript()))).isInstanceOf(IOException.class);
    }
}

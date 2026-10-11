package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.rewriteInPlace;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.textOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.writeFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TranscriptLinesTest {

    @TempDir
    Path directory;

    @Test
    void shouldHandEachCompleteLineAndReturnTheOffsetAfterTheLastNewline() throws IOException {
        Path file = writeFile(directory, "one\ntwo\n");
        List<String> lines = new ArrayList<>();

        long offset = TranscriptLines.read(file, 0, line -> lines.add(textOf(line)));

        assertThat(lines).containsExactly("one", "two");
        assertThat(offset).isEqualTo(8);
    }

    @Test
    void shouldLeaveALastLineWithNoNewlineForTheNextRead() throws IOException {
        Path file = writeFile(directory, "one\ntw");
        List<String> lines = new ArrayList<>();

        long offset = TranscriptLines.read(file, 0, line -> lines.add(textOf(line)));

        assertThat(lines).containsExactly("one");
        assertThat(offset).as("the offset stays before the half-written line").isEqualTo(4);
    }

    @Test
    void shouldTakeTheLateLineWholeOnceItsNewlineArrives() throws IOException {
        Path file = writeFile(directory, "one\ntw");
        long offset = TranscriptLines.read(file, 0, line -> {});
        rewriteInPlace(file, "one\ntwo\n");
        List<String> lines = new ArrayList<>();

        long next = TranscriptLines.read(file, offset, line -> lines.add(textOf(line)));

        assertThat(lines).containsExactly("two");
        assertThat(next).isEqualTo(8);
    }

    @Test
    void shouldCountBytesNotCharactersInTheOffset() throws IOException {
        Path file = writeFile(directory, "é\nb\n");
        List<String> lines = new ArrayList<>();

        long offset = TranscriptLines.read(file, 0, line -> lines.add(textOf(line)));

        assertThat(lines).containsExactly("é", "b");
        assertThat(offset).as("é is two bytes").isEqualTo(5);
    }

    @Test
    void shouldReadALineLongerThanOneChunkWhole() throws IOException {
        String longLine = "x".repeat(200_000);
        Path file = writeFile(directory, longLine + "\nnext\n");
        List<String> lines = new ArrayList<>();

        TranscriptLines.read(file, 0, line -> lines.add(textOf(line)));

        assertThat(lines).containsExactly(longLine, "next");
    }

    @Test
    void shouldReturnTheStartingOffsetForAFileWithNothingNew() throws IOException {
        Path file = writeFile(directory, "one\n");

        assertThat(TranscriptLines.read(file, 4, line -> {})).isEqualTo(4);
    }
}

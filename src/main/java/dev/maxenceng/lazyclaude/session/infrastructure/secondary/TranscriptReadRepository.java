package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.NoTranscript;
import dev.maxenceng.lazyclaude.session.domain.ReadEntries;
import dev.maxenceng.lazyclaude.session.domain.TranscriptRead;
import dev.maxenceng.lazyclaude.session.domain.TranscriptReadPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptUnreadableException;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TranscriptEntry;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Reads the entries of a transcript, {@code ~/.claude/projects/<folder>/<sessionId>.jsonl}, from a byte offset. The folder
 * is the session's directory with every character that is not a letter or a digit written as {@code -}; a session that moved
 * directory is found by its id in any folder. This is a second read of the file beside the one that links sessions
 * to tickets (the cache), and keeps nothing: the offset it hands back is the caller's to keep.
 */
// deferred: the first read of a transcript parses all of it, which a very long session makes slow — close it when showing one is seen to lag.
public final class TranscriptReadRepository implements TranscriptReadPort {

    private static final String SUFFIX = ".jsonl";
    private static final String NOT_A_FOLDER_NAME_CHARACTER = "[^A-Za-z0-9]";
    private static final String FOLDER_NAME_SEPARATOR = "-";
    private static final String UNREADABLE = "cannot read %s: %s";

    private final Path transcriptsRoot;

    public TranscriptReadRepository(Path transcriptsRoot) {
        this.transcriptsRoot = transcriptsRoot;
    }

    @Override
    public TranscriptRead read(SessionId session, WorkingDirectory directory, TranscriptOffset from) {
        try {
            Optional<Path> file = locate(session, directory);
            return file.isPresent() ? readFrom(file.get(), from) : new NoTranscript();
        } catch (IOException e) {
            throw new TranscriptUnreadableException(UNREADABLE.formatted(transcriptsRoot, FileFailure.causeOf(e)), e);
        } catch (UncheckedIOException e) {
            throw new TranscriptUnreadableException(
                    UNREADABLE.formatted(transcriptsRoot, FileFailure.causeOf(e.getCause())), e.getCause());
        }
    }

    private Optional<Path> locate(SessionId session, WorkingDirectory directory) throws IOException {
        String name = session.value() + SUFFIX;
        Path expected = transcriptsRoot
                .resolve(directory.path().toString().replaceAll(NOT_A_FOLDER_NAME_CHARACTER, FOLDER_NAME_SEPARATOR))
                .resolve(name);
        if (Files.isRegularFile(expected)) {
            return Optional.of(expected);
        }
        if (Files.notExists(transcriptsRoot)) {
            return Optional.empty();
        }
        try (Stream<Path> folders = Files.list(transcriptsRoot)) {
            return folders.map(folder -> folder.resolve(name)).filter(Files::isRegularFile).findFirst();
        }
    }

    private static TranscriptRead readFrom(Path file, TranscriptOffset from) throws IOException {
        TranscriptOffset start = from.value() > Files.size(file) ? TranscriptOffset.start() : from;
        Stream.Builder<TranscriptEntry> entries = Stream.builder();
        long next = TranscriptLines.read(file, start.value(), line -> TranscriptEntryLine.entriesIn(line).forEach(entries));
        return ReadEntries.builder()
                .entries(entries.build().toList())
                .start(start)
                .next(new TranscriptOffset(next))
                .build();
    }
}

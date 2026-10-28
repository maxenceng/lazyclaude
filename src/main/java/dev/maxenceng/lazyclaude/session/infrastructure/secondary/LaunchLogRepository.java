package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.domain.LaunchEntry;
import dev.maxenceng.lazyclaude.session.domain.LaunchLog;
import dev.maxenceng.lazyclaude.session.domain.LaunchLogFallback;
import dev.maxenceng.lazyclaude.session.domain.LaunchLogPort;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps one launch log per project in a folder of lazyclaude's own, a line per launch. A missing log is an empty one. A change
 * reads the log, applies the change and writes the log whole to a temporary file beside it that is renamed over it, so a reader
 * never meets half a log; changes run one at a time. A log that cannot be read, or has a line that cannot be used, is never
 * written over: it is the only record of what it held, and the message names the file and the line. A session logged on two lines
 * is such a line, the second of them.
 */
public final class LaunchLogRepository implements LaunchLogPort {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String FILE = "launch-log-%s-%08x.jsonl";
    private static final String TEMPORARY_SUFFIX = ".tmp";
    private static final String CANNOT_READ = "cannot read %s: %s";
    private static final String CANNOT_WRITE = "cannot write %s: %s";
    private static final String UNUSABLE_LINE = "%s:%d: %s";
    private static final String NOT_JSON = "not a JSON object (%s)";
    private static final String LOGGED_TWICE = "session %s is logged twice";

    private final Path directory;
    private final Object changing = new Object();
    private final AtomicBoolean said = new AtomicBoolean();

    public LaunchLogRepository(Path directory) {
        this.directory = directory;
    }

    @Override
    public LaunchLog read(Project project, LaunchLogFallback fallback) {
        try {
            return read(fileOf(project));
        } catch (LaunchLogFailureException unavailable) {
            said(fallback, unavailable);
            return LaunchLog.builder().build();
        }
    }

    @Override
    public void change(Project project, LaunchLogFallback fallback, UnaryOperator<LaunchLog> change) {
        Path file = fileOf(project);
        synchronized (changing) {
            try {
                write(file, change.apply(read(file)));
            } catch (LaunchLogFailureException unavailable) {
                said(fallback, unavailable);
            }
        }
    }

    /** The first failure since this repository was built is told to {@code fallback}; every later one is already said. */
    private void said(LaunchLogFallback fallback, LaunchLogFailureException unavailable) {
        if (said.compareAndSet(false, true)) {
            fallback.unavailable(new Reason(unavailable.getMessage()));
        }
    }

    Path fileOf(Project project) {
        Path name = Optional.ofNullable(project.root().getFileName()).orElse(project.root());
        return directory.resolve(FILE.formatted(name, project.root().toString().hashCode()));
    }

    private static LaunchLog read(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException missing) {
            return LaunchLog.builder().build();
        } catch (IOException e) {
            throw new LaunchLogFailureException(CANNOT_READ.formatted(file, FileFailure.causeOf(e)), e);
        }
        IntStream.range(0, lines.size())
                .mapToObj(index -> flawOf(lines.get(index)).map(flaw -> UNUSABLE_LINE.formatted(file, index + 1, flaw)))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(flaw -> {
                    throw new LaunchLogFailureException(flaw);
                });
        List<Optional<LaunchEntry>> entries = lines.stream()
                .map(line -> line.isBlank() ? Optional.<LaunchEntry>empty() : parsed(line).toEntry())
                .toList();
        IntStream.range(0, entries.size())
                .mapToObj(index -> entries.get(index)
                        .filter(entry -> loggedBefore(entries, index, entry.id()))
                        .map(entry -> UNUSABLE_LINE.formatted(file, index + 1, LOGGED_TWICE.formatted(entry.id().value()))))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(flaw -> {
                    throw new LaunchLogFailureException(flaw);
                });
        return LaunchLog.builder()
                .entries(entries.stream().flatMap(Optional::stream).toList())
                .build();
    }

    private static boolean loggedBefore(List<Optional<LaunchEntry>> entries, int index, SessionId id) {
        return entries.subList(0, index).stream().flatMap(Optional::stream).anyMatch(entry -> entry.id().equals(id));
    }

    /** Why {@code text} is no launch, none for a blank line or one that is. */
    private static Optional<String> flawOf(String text) {
        if (text.isBlank()) {
            return Optional.empty();
        }
        try {
            LaunchLogLine line = parsed(text);
            return line == null ? Optional.of(NOT_JSON.formatted(text)) : line.flaw();
        } catch (JacksonException notJson) {
            return Optional.of(NOT_JSON.formatted(notJson.getOriginalMessage()));
        }
    }

    private static LaunchLogLine parsed(String text) {
        return MAPPER.readValue(text, LaunchLogLine.class);
    }

    private static void write(Path file, LaunchLog log) {
        Path temporary = file.resolveSibling(file.getFileName() + TEMPORARY_SUFFIX);
        try {
            String content = log.entries().stream()
                    .map(LaunchLogRepository::serialised)
                    .map(line -> line + System.lineSeparator())
                    .collect(Collectors.joining());
            Files.createDirectories(file.getParent());
            try {
                Files.writeString(temporary, content, StandardCharsets.UTF_8);
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new LaunchLogFailureException(CANNOT_WRITE.formatted(file, FileFailure.causeOf(e)), e);
        }
    }

    private static String serialised(LaunchEntry entry) {
        return MAPPER.writeValueAsString(LaunchLogLine.of(entry));
    }
}

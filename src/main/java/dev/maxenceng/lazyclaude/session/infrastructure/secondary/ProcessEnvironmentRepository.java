package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.EnvironmentUnreadableException;
import dev.maxenceng.lazyclaude.session.domain.EnvironmentVariableName;
import dev.maxenceng.lazyclaude.session.domain.EnvironmentVariableValue;
import dev.maxenceng.lazyclaude.session.domain.ProcessEnvironment;
import dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentPort;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/**
 * Reads a process's environment from {@code /proc/<pid>/environ}, NUL-separated {@code NAME=value} entries as the process
 * was started with them. A process that is gone has none; one that is there but unreadable is a failure. lazyclaude's
 * own environment is the one it was started with, handed over at wiring.
 */
public final class ProcessEnvironmentRepository implements ProcessEnvironmentPort {

    private static final String ENVIRON = "environ";
    private static final String ENTRY_SEPARATOR = "\0";
    private static final String NAME_SEPARATOR = "=";
    private static final int NAME_AND_VALUE = 2;
    private static final String UNREADABLE = "cannot read %s: %s";

    private final Path proc;
    private final Map<String, String> own;

    public ProcessEnvironmentRepository(Path proc, Map<String, String> own) {
        this.proc = proc;
        this.own = own;
    }

    @Override
    public ProcessEnvironment of(ProcessId pid) {
        Path file = proc.resolve(Long.toString(pid.value())).resolve(ENVIRON);
        try {
            String entries = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return environmentOf(Stream.of(entries.split(ENTRY_SEPARATOR))
                    .map(entry -> entry.split(NAME_SEPARATOR, NAME_AND_VALUE))
                    .filter(parts -> parts.length == NAME_AND_VALUE)
                    .map(parts -> Map.entry(parts[0], parts[1])));
        } catch (NoSuchFileException gone) {
            return environmentOf(Stream.empty());
        } catch (IOException e) {
            throw new EnvironmentUnreadableException(UNREADABLE.formatted(file, FileFailure.causeOf(e)), e);
        }
    }

    @Override
    public ProcessEnvironment ownEnvironment() {
        return environmentOf(own.entrySet().stream());
    }

    private static ProcessEnvironment environmentOf(Stream<Map.Entry<String, String>> variables) {
        return new ProcessEnvironment(variables
                .filter(variable -> !variable.getKey().isBlank() && !variable.getValue().isBlank())
                .collect(Collectors.toMap(
                        variable -> new EnvironmentVariableName(variable.getKey()),
                        variable -> new EnvironmentVariableValue(variable.getValue()),
                        (first, second) -> second)));
    }
}

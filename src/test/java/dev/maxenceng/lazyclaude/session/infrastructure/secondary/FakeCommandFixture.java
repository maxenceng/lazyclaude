package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

public final class FakeCommandFixture {

    private FakeCommandFixture() {}

    public static Path commandPrinting(Path directory, String name, String output) throws IOException {
        return script(directory, name, "echo \"$@\" >> '%s'\necho '%s'\n".formatted(argumentsOf(directory, name), output));
    }

    public static Path commandFailing(Path directory, String name, int exitCode, String complaint) throws IOException {
        return script(
                directory,
                name,
                "echo \"$@\" >> '%s'\necho '%s' >&2\nexit %d\n".formatted(argumentsOf(directory, name), complaint, exitCode));
    }

    public static Path commandHanging(Path directory, String name) throws IOException {
        return script(directory, name, "exec sleep 30\n");
    }

    public static Path commandPrintingNothing(Path directory, String name) throws IOException {
        return script(directory, name, "echo \"$@\" >> '%s'\n".formatted(argumentsOf(directory, name)));
    }

    public static List<String> argumentsGiven(Path directory, String name) throws IOException {
        return Files.readAllLines(argumentsOf(directory, name));
    }

    private static Path argumentsOf(Path directory, String name) {
        return directory.resolve(name + ".arguments");
    }

    private static Path script(Path directory, String name, String body) throws IOException {
        Path script = directory.resolve(name);
        Files.writeString(script, "#!/bin/sh\n" + body, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        return script;
    }
}

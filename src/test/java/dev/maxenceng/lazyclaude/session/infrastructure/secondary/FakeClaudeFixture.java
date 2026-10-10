package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public final class FakeClaudeFixture {

    private FakeClaudeFixture() {}

    public static Path claudePrinting(Path directory, String output) throws IOException {
        return script(directory, "cat <<'JSON'\n%s\nJSON\n".formatted(output));
    }

    public static Path claudeFailing(Path directory, int exitCode) throws IOException {
        return script(directory, "echo 'not signed in' >&2\nexit %d\n".formatted(exitCode));
    }

    public static Path claudeHanging(Path directory) throws IOException {
        return script(directory, "exec sleep 30\n");
    }

    public static String missingCommand() {
        return "lazyclaude-no-such-claude";
    }

    private static Path script(Path directory, String body) throws IOException {
        Path script = directory.resolve("claude");
        Files.writeString(script, "#!/bin/sh\n" + body, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        return script;
    }
}

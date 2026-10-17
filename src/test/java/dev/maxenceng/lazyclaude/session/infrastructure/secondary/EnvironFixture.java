package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EnvironFixture {

    private static final char NUL = '\0';

    private EnvironFixture() {}

    public static void environOf(Path proc, long pid, String... entries) throws IOException {
        Path process = Files.createDirectories(proc.resolve(Long.toString(pid)));
        Files.writeString(
                process.resolve("environ"),
                Stream.of(entries).map(entry -> entry + NUL).collect(Collectors.joining()),
                StandardCharsets.UTF_8);
    }

    public static void environThatIsADirectory(Path proc, long pid) throws IOException {
        Files.createDirectories(proc.resolve(Long.toString(pid)).resolve("environ"));
    }
}

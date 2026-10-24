package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.ByteArrayOutputStream;
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

    public static void environWithAByteThatIsNotUtf8(Path proc, long pid, String before, String after) throws IOException {
        Path process = Files.createDirectories(proc.resolve(Long.toString(pid)));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.writeBytes((before + NUL).getBytes(StandardCharsets.UTF_8));
        bytes.writeBytes(new byte[] {'L', 'A', 'N', 'G', '=', (byte) 0xE9, NUL});
        bytes.writeBytes((after + NUL).getBytes(StandardCharsets.UTF_8));
        Files.write(process.resolve("environ"), bytes.toByteArray());
    }

    public static void environThatIsADirectory(Path proc, long pid) throws IOException {
        Files.createDirectories(proc.resolve(Long.toString(pid)).resolve("environ"));
    }
}

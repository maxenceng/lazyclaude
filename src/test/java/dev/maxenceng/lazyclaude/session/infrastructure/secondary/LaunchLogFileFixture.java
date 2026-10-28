package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LaunchLogFileFixture {

    private LaunchLogFileFixture() {}

    static Path writtenLaunchLog(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content + System.lineSeparator(), StandardCharsets.UTF_8);
        return file;
    }
}

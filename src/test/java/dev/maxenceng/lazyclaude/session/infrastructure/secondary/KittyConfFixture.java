package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.session.domain.KittyConfigFile;

public final class KittyConfFixture {

    private KittyConfFixture() {}

    public static Path confFile(Path directory, String name, String... lines) throws IOException {
        Path file = directory.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return file;
    }

    public static Path confFileWithoutFinalNewline(Path directory, String name, String... lines) throws IOException {
        Path file = directory.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("\n", lines), StandardCharsets.UTF_8);
        return file;
    }

    public static KittyConfigFile configFile(Path directory, String name) {
        return new KittyConfigFile(directory.resolve(name));
    }

    public static String textOf(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}

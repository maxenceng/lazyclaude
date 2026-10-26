package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigYmlFixture {

    private ConfigYmlFixture() {}

    public static Path configYml(Path configHome, String... lines) throws IOException {
        Path file = configHome.resolve("lazyclaude/config.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return file;
    }

    public static Path configYmlLinkingToNothing(Path configHome) throws IOException {
        Path file = configHome.resolve("lazyclaude/config.yml");
        Files.createDirectories(file.getParent());
        return Files.createSymbolicLink(file, configHome.resolve("dotfiles/config.yml"));
    }

    public static Path configYmlInLatin1(Path configHome) throws IOException {
        Path file = configHome.resolve("lazyclaude/config.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "# caf\u00e9\nkeybinding:\n  zoom: alt+z\n", StandardCharsets.ISO_8859_1);
        return file;
    }

    public static Path keybindingsJsonLinkingToNothing(Path home) throws IOException {
        Path file = home.resolve(".claude/keybindings.json");
        Files.createDirectories(file.getParent());
        return Files.createSymbolicLink(file, home.resolve("dotfiles/keybindings.json"));
    }

    public static Path keybindingsJson(Path home, String text) throws IOException {
        Path file = home.resolve(".claude/keybindings.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.Configuration;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationPort;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationRefusedException;

/**
 * Finds and reads {@code config.yml}, under {@code $XDG_CONFIG_HOME/lazyclaude/} or {@code ~/.config/lazyclaude/} when that is
 * unset, as the XDG base directory spec has it. A missing file is the defaults, but a link to a missing one is refused:
 * its rebindings were meant to apply. lazyclaude never writes it.
 */
public final class ConfigurationRepository implements ConfigurationPort {

    private static final String XDG_CONFIG_HOME = "XDG_CONFIG_HOME";
    private static final String DEFAULT_CONFIG_HOME = ".config";
    private static final String DIRECTORY = "lazyclaude";
    private static final String FILE = "config.yml";
    private static final String HOME = "~";
    private static final String CANNOT_READ = "%s: cannot read: %s";
    private static final String LINKS_TO_NOTHING = "%s: links to a file that does not exist";

    private final Path home;
    private final Map<String, String> environment;

    public ConfigurationRepository(Path home, Map<String, String> environment) {
        this.home = home;
        this.environment = environment;
    }

    @Override
    public Configuration configuration() {
        Path file = file();
        String shown = shown(file);
        try {
            return new ConfigurationNodes(shown).read(Files.readString(file, StandardCharsets.UTF_8));
        } catch (NoSuchFileException missing) {
            if (Files.isSymbolicLink(file)) {
                throw new ConfigurationRefusedException(LINKS_TO_NOTHING.formatted(shown));
            }
            return ConfigurationNodes.defaults();
        } catch (IOException e) {
            throw new ConfigurationRefusedException(CANNOT_READ.formatted(shown, FileFailure.causeOf(e)));
        }
    }

    private Path file() {
        return Optional.ofNullable(environment.get(XDG_CONFIG_HOME))
                .filter(set -> !set.isBlank())
                .map(set -> Path.of(set).toAbsolutePath())
                .orElseGet(() -> home.resolve(DEFAULT_CONFIG_HOME))
                .resolve(DIRECTORY)
                .resolve(FILE);
    }

    private String shown(Path file) {
        return file.startsWith(home) ? Path.of(HOME).resolve(home.relativize(file)).toString() : file.toString();
    }
}

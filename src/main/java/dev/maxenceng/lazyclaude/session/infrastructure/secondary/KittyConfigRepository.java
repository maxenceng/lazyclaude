package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.ConfigKey;
import dev.maxenceng.lazyclaude.session.domain.ConfigLine;
import dev.maxenceng.lazyclaude.session.domain.ConfigLineNumber;
import dev.maxenceng.lazyclaude.session.domain.ConfigPlace;
import dev.maxenceng.lazyclaude.session.domain.ConfigSetting;
import dev.maxenceng.lazyclaude.session.domain.ConfigValue;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigFile;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigPort;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigUnreadableException;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigUnwritableException;
import dev.maxenceng.lazyclaude.session.domain.KittySettings;

/**
 * Finds, reads and adds to {@code kitty.conf}. A line is {@code key value}; {@code include} and {@code globinclude} pull in other
 * files where they are written, relative to the file that names them, and a later line overrides an earlier one, which is the
 * order this hands the settings over in. A file already being read is not read again, so an include cycle ends, and an
 * included file that does not exist is skipped, as kitty skips it.
 */
public final class KittyConfigRepository implements KittyConfigPort {

    private static final String CONFIG_DIRECTORY = "KITTY_CONFIG_DIRECTORY";
    private static final String CONFIG_FILE = "kitty.conf";
    private static final String DEFAULT_DIRECTORY = ".config/kitty";
    private static final String COMMENT = "#";
    private static final String INCLUDE = "include";
    private static final String GLOBINCLUDE = "globinclude";
    private static final String HOME_PREFIX = "~/";
    private static final String WHITESPACE = "\\s+";
    private static final int KEY_AND_VALUE = 2;
    private static final String GLOB_CHARACTERS = "*?[{";
    private static final String GLOB = "glob:%s";
    private static final String RECURSIVE_GLOB = "**";
    private static final String ADDED_BY = "# added by lazyclaude";
    private static final String LINE = "%s %s";
    private static final String NEWLINE = "\n";
    private static final String CANNOT_READ = "cannot read %s: %s";
    private static final String CANNOT_WRITE = "cannot write %s: %s";

    private final Path home;
    private final Map<String, String> environment;

    public KittyConfigRepository(Path home, Map<String, String> environment) {
        this.home = home;
        this.environment = environment;
    }

    @Override
    public KittyConfigFile locate() {
        Path directory = Optional.ofNullable(environment.get(CONFIG_DIRECTORY))
                .filter(set -> !set.isBlank())
                .map(set -> Path.of(set).toAbsolutePath())
                .orElseGet(() -> home.resolve(DEFAULT_DIRECTORY));
        return new KittyConfigFile(directory.resolve(CONFIG_FILE));
    }

    @Override
    public KittySettings settings(KittyConfigFile file, List<ConfigKey> keys) {
        return new KittySettings(settingsOf(file.path(), keys, Set.of()).toList());
    }

    @Override
    public void append(KittyConfigFile file, List<ConfigLine> lines) {
        try {
            Files.createDirectories(file.path().getParent());
            String existing = existingText(file.path());
            String separator = existing.isEmpty() ? "" : existing.endsWith(NEWLINE) ? NEWLINE : NEWLINE + NEWLINE;
            String added = Stream.concat(
                            Stream.of(ADDED_BY),
                            lines.stream().map(line -> LINE.formatted(line.key().value(), line.value().value())))
                    .collect(Collectors.joining(NEWLINE, separator, NEWLINE));
            Files.writeString(
                    file.path(), added, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new KittyConfigUnwritableException(CANNOT_WRITE.formatted(file.path(), FileFailure.causeOf(e)), e);
        }
    }

    private static String existingText(Path file) throws IOException {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException missing) {
            return "";
        }
    }

    private Stream<ConfigSetting> settingsOf(Path file, List<ConfigKey> keys, Set<Path> reading) {
        if (reading.contains(file)) {
            return Stream.empty();
        }
        Set<Path> chain = Stream.concat(reading.stream(), Stream.of(file)).collect(Collectors.toUnmodifiableSet());
        List<String> lines = linesOf(file);
        return IntStream.range(0, lines.size())
                .boxed()
                .flatMap(index -> settingsOfLine(file, index + 1, lines.get(index).strip(), keys, chain));
    }

    private Stream<ConfigSetting> settingsOfLine(Path file, int number, String line, List<ConfigKey> keys, Set<Path> reading) {
        if (line.isEmpty() || line.startsWith(COMMENT)) {
            return Stream.empty();
        }
        String[] parts = line.split(WHITESPACE, KEY_AND_VALUE);
        if (parts.length < KEY_AND_VALUE) {
            return Stream.empty();
        }
        String value = parts[1].strip();
        return switch (parts[0]) {
            case INCLUDE -> includedBy(file, value).flatMap(included -> settingsOf(included, keys, reading));
            case GLOBINCLUDE -> globbedBy(file, value).flatMap(included -> settingsOf(included, keys, reading));
            default -> keys.stream()
                    .filter(key -> key.value().equals(parts[0]))
                    .map(key -> ConfigSetting.builder()
                            .line(ConfigLine.builder().key(key).value(new ConfigValue(value)).build())
                            .place(ConfigPlace.builder()
                                    .file(new KittyConfigFile(file))
                                    .line(new ConfigLineNumber(number))
                                    .build())
                            .build());
        };
    }

    private Stream<Path> includedBy(Path file, String path) {
        return Stream.of(resolved(file, path));
    }

    private Stream<Path> globbedBy(Path file, String pattern) {
        Path absolute = resolved(file, pattern);
        Path base = absolute.getRoot();
        Path prefix = IntStream.range(0, absolute.getNameCount())
                .mapToObj(absolute::getName)
                .takeWhile(name -> GLOB_CHARACTERS.chars().noneMatch(glob -> name.toString().indexOf(glob) >= 0))
                .reduce(base, Path::resolve);
        int depth = pattern.contains(RECURSIVE_GLOB) ? Integer.MAX_VALUE : absolute.getNameCount() - prefix.getNameCount();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(GLOB.formatted(absolute));
        try (Stream<Path> found = Files.walk(prefix, depth)) {
            return found.filter(Files::isRegularFile).filter(matcher::matches).sorted().toList().stream();
        } catch (NoSuchFileException nothingThere) {
            return Stream.empty();
        } catch (IOException e) {
            throw new KittyConfigUnreadableException(CANNOT_READ.formatted(prefix, FileFailure.causeOf(e)), e);
        } catch (UncheckedIOException e) {
            throw new KittyConfigUnreadableException(CANNOT_READ.formatted(prefix, FileFailure.causeOf(e.getCause())), e.getCause());
        }
    }

    private Path resolved(Path file, String path) {
        Path wanted = path.startsWith(HOME_PREFIX) ? home.resolve(path.substring(HOME_PREFIX.length())) : Path.of(path);
        return file.getParent().resolve(wanted).normalize();
    }

    private static List<String> linesOf(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException missing) {
            return List.of();
        } catch (IOException e) {
            throw new KittyConfigUnreadableException(CANNOT_READ.formatted(file, FileFailure.causeOf(e)), e);
        }
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.ClaudeKeybindingsPort;
import dev.maxenceng.lazyclaude.session.domain.ClaudeKeybindingsUnreadableException;
import dev.maxenceng.lazyclaude.session.published.Chord;

import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the keystrokes the user bound in {@code ~/.claude/keybindings.json}: an object whose {@code bindings} are blocks of a
 * context and a map of keystroke to action. A keystroke bound to {@code null} unbinds a default, so it binds nothing. A missing
 * file binds nothing; one that cannot be read, parsed or is not of that shape says so, and the caller carries on without the
 * check, since a check that read nothing from it would report no clash it had not looked for. Claude Code's own defaults are spelled
 * here as its keybindings documentation lists them, which {@code make contract} compares with the published page.
 */
public final class ClaudeKeybindingsRepository implements ClaudeKeybindingsPort {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String CLAUDE_DIRECTORY = ".claude";
    private static final String FILE = "keybindings.json";
    private static final String BINDINGS = "bindings";
    private static final String CANNOT_READ = "cannot read %s: %s";
    private static final String CANNOT_PARSE = "cannot parse %s: %s";
    private static final String CANNOT_PARSE_AT = "cannot parse %s:%d:%d: %s";
    private static final String NOT_BINDINGS = "%s is not an object with a list of bindings";
    private static final String NOT_A_BLOCK = "%s has a binding block that is not an object of bindings";
    private static final String NOT_AN_ACTION = "%s binds '%s' to something that is neither an action nor null";
    private static final String LINKS_TO_NOTHING = "%s links to a file that does not exist";
    private static final int NO_LINE = 0;
    // deferred: a default the user unbound with null in keybindings.json is still reported as Claude Code's — close it when someone frees a default for a reserved chord.
    private static final List<String> DEFAULTS = List.of(
            "ctrl+b", "ctrl+c", "ctrl+d", "ctrl+e", "ctrl+f", "ctrl+g", "ctrl+j", "ctrl+l", "ctrl+n", "ctrl+o", "ctrl+p", "ctrl+r",
            "ctrl+s", "ctrl+t", "ctrl+v", "ctrl+x", "ctrl+_", "ctrl+shift+-", "ctrl+shift+c", "ctrl+up", "ctrl+down", "ctrl+home",
            "ctrl+end", "ctrl+enter", "meta+o", "meta+p", "meta+t", "meta+up", "meta+down", "alt+v");

    private final Path file;

    public ClaudeKeybindingsRepository(Path home) {
        this.file = home.resolve(CLAUDE_DIRECTORY).resolve(FILE);
    }

    @Override
    public List<Chord> bindings() {
        try {
            return chordsIn(MAPPER.readTree(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (NoSuchFileException missing) {
            if (Files.isSymbolicLink(file)) {
                throw new ClaudeKeybindingsUnreadableException(LINKS_TO_NOTHING.formatted(file), missing);
            }
            return List.of();
        } catch (IOException e) {
            throw new ClaudeKeybindingsUnreadableException(CANNOT_READ.formatted(file, FileFailure.causeOf(e)), e);
        } catch (JacksonException e) {
            throw new ClaudeKeybindingsUnreadableException(unparsed(e), e);
        }
    }

    @Override
    public List<Chord> defaults() {
        return DEFAULTS.stream().map(ChordSpelling::boundByClaude).flatMap(Optional::stream).toList();
    }

    private String unparsed(JacksonException failure) {
        return Optional.ofNullable(failure.getLocation())
                .filter(location -> location.getLineNr() > NO_LINE)
                .map(location -> located(location, failure.getOriginalMessage()))
                .orElseGet(() -> CANNOT_PARSE.formatted(file, failure.getOriginalMessage()));
    }

    private String located(TokenStreamLocation location, String reason) {
        return CANNOT_PARSE_AT.formatted(file, location.getLineNr(), location.getColumnNr(), reason);
    }

    private List<Chord> chordsIn(JsonNode root) {
        JsonNode blocks = root.path(BINDINGS);
        if (!root.isObject() || !blocks.isArray()) {
            throw malformed(NOT_BINDINGS.formatted(file));
        }
        return blocks.valueStream()
                .flatMap(this::boundIn)
                .map(ChordSpelling::boundByClaude)
                .flatMap(Optional::stream)
                .toList();
    }

    private Stream<String> boundIn(JsonNode block) {
        JsonNode bindings = block.path(BINDINGS);
        if (!block.isObject() || !bindings.isObject()) {
            throw malformed(NOT_A_BLOCK.formatted(file));
        }
        return bindings.properties().stream()
                .filter(this::bindsAnAction)
                .map(Map.Entry::getKey);
    }

    private boolean bindsAnAction(Map.Entry<String, JsonNode> binding) {
        if (!binding.getValue().isString() && !binding.getValue().isNull()) {
            throw malformed(NOT_AN_ACTION.formatted(file, binding.getKey()));
        }
        return binding.getValue().isString();
    }

    private static ClaudeKeybindingsUnreadableException malformed(String reason) {
        return new ClaudeKeybindingsUnreadableException(reason);
    }
}

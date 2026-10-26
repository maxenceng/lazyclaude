package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException;
import org.snakeyaml.engine.v2.exceptions.ReaderException;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.Tag;

import dev.maxenceng.lazyclaude.session.domain.Configuration;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationRefusedException;
import dev.maxenceng.lazyclaude.session.published.Chord;
import dev.maxenceng.lazyclaude.session.published.DuplicateChordException;
import dev.maxenceng.lazyclaude.session.published.InvalidChordException;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.maxenceng.lazyclaude.session.published.TakenChordException;

/**
 * Reads the text of {@code config.yml} node by node, so that every refusal can name the line it is on. SnakeYAML Engine's YAML 1.2
 * keeps {@code no} a string, so a boolean is only {@code true} or {@code false}; a key that is not a name is let through to be
 * refused here, at its line, rather than by the parser at none. Nothing is bound to a class: an entry the
 * configuration does not know is refused, never skipped, since a typo would quietly bring back the chord the file exists to escape.
 */
final class ConfigurationNodes {

    private static final String KEYBINDING = "keybinding";
    private static final String NOTIFICATIONS = "notifications";
    private static final String DESKTOP = "desktop";
    private static final Map<String, ReservedAction> ACTIONS = Arrays.stream(ReservedAction.values())
            .collect(Collectors.toUnmodifiableMap(ReservedAction::configurationName, Function.identity()));
    private static final List<String> SECTIONS = List.of(KEYBINDING, NOTIFICATIONS);
    private static final int FIRST_LINE = 1;
    private static final int NO_LINE = 0;
    private static final String LOCATED = "%s:%d: %s";
    private static final String UNKNOWN_KEY = "unknown key '%s'";
    private static final String NOT_A_NAME = "a key must be a name";
    private static final String SPECIAL_CHARACTER = "character U+%04X is not allowed";
    private static final int NEWLINE = '\n';
    private static final String DUPLICATE_KEY = "duplicate key '%s'";
    private static final String NOT_ENTRIES = "expected entries such as keybinding or notifications";
    private static final String NOT_A_SECTION = "%s needs entries of its own";
    private static final String NEEDS_A_CHORD = "%s needs a chord such as %s";
    private static final String NOT_A_BOOLEAN = "'%s' is not a valid value for %s: use true or false";
    private static final String NEEDS_A_BOOLEAN = "%s needs true or false";
    private static final String ALREADY_BOUND = "%s is already bound to %s";
    private static final String TAKEN = "%s is lazyclaude's %s";
    private static final String ON_LINE = " on line %d";
    private static final String BY_DEFAULT = " by default";

    private final String file;
    private final Compose compose = new Compose(LoadSettings.builder().setAllowNonScalarKeys(true).build());

    ConfigurationNodes(String file) {
        this.file = file;
    }

    Configuration read(String text) {
        List<ConfigurationEntry> top = entriesOf(parsed(text), SECTIONS);
        List<ConfigurationEntry> bindings = section(top, KEYBINDING, ACTIONS.keySet());
        List<ConfigurationEntry> notifications = section(top, NOTIFICATIONS, List.of(DESKTOP));
        return Configuration.builder()
                .reservedChords(reserved(bindings))
                .desktopNotifications(notifications.stream().findFirst().map(this::desktop).orElseGet(() -> Configuration.defaults().desktopNotifications()))
                .build();
    }

    private Optional<Node> parsed(String text) {
        try {
            return compose.composeString(text);
        } catch (ReaderException broken) {
            throw refused(lineAt(text, broken.getPosition()), SPECIAL_CHARACTER.formatted(broken.getCodePoint()));
        } catch (MarkedYamlEngineException broken) {
            throw refused(lineOf(broken.getProblemMark().or(broken::getContextMark)), broken.getProblem());
        } catch (YamlEngineException broken) {
            throw refused(FIRST_LINE, broken.getMessage());
        }
    }

    private List<ConfigurationEntry> entriesOf(Optional<Node> node, Collection<String> known) {
        return node.map(root -> entriesOfMapping(root, known, NOT_ENTRIES)).orElse(List.of());
    }

    private List<ConfigurationEntry> entriesOfMapping(Node node, Collection<String> known, String notMapping) {
        if (!(node instanceof MappingNode mapping)) {
            throw refused(lineOf(node.getStartMark()), notMapping);
        }
        List<ConfigurationEntry> entries = mapping.getValue().stream().map(tuple -> entryOf(tuple, known)).toList();
        IntStream.range(0, entries.size())
                .filter(index -> entries.subList(0, index).stream()
                        .anyMatch(earlier -> earlier.key().equals(entries.get(index).key())))
                .findFirst()
                .ifPresent(index -> {
                    throw refused(entries.get(index).line(), DUPLICATE_KEY.formatted(entries.get(index).key()));
                });
        return entries;
    }

    private ConfigurationEntry entryOf(NodeTuple tuple, Collection<String> known) {
        int line = lineOf(tuple.getKeyNode().getStartMark());
        if (!(tuple.getKeyNode() instanceof ScalarNode scalar)) {
            throw refused(line, NOT_A_NAME);
        }
        String key = scalar.getValue();
        if (!known.contains(key)) {
            throw refused(line, UNKNOWN_KEY.formatted(key));
        }
        return new ConfigurationEntry(key, line, tuple.getValueNode());
    }

    private List<ConfigurationEntry> section(List<ConfigurationEntry> top, String name, Collection<String> known) {
        return top.stream()
                .filter(entry -> entry.key().equals(name))
                .findFirst()
                .map(entry -> entriesOfMapping(entry.value(), known, NOT_A_SECTION.formatted(name)))
                .orElse(List.of());
    }

    private ReservedChords reserved(List<ConfigurationEntry> bindings) {
        Map<ReservedAction, Chord> chords = bindings.stream()
                .collect(Collectors.toMap(entry -> ACTIONS.get(entry.key()), this::chordOf));
        Map<ReservedAction, Integer> lines = bindings.stream()
                .collect(Collectors.toMap(entry -> ACTIONS.get(entry.key()), ConfigurationEntry::line));
        try {
            return ReservedChords.defaults().rebinding(chords);
        } catch (DuplicateChordException duplicate) {
            throw duplicated(duplicate, lines);
        } catch (TakenChordException taken) {
            throw refused(lines.get(taken.action()), TAKEN.formatted(taken.fixed().chord().text(), taken.fixed().purpose()));
        }
    }

    private ConfigurationRefusedException duplicated(DuplicateChordException duplicate, Map<ReservedAction, Integer> lines) {
        ReservedAction second = lines.getOrDefault(duplicate.first(), NO_LINE) > lines.getOrDefault(duplicate.second(), NO_LINE)
                ? duplicate.first()
                : duplicate.second();
        ReservedAction first = second == duplicate.first() ? duplicate.second() : duplicate.first();
        String where = lines.containsKey(first) ? ON_LINE.formatted(lines.get(first)) : BY_DEFAULT;
        return refused(
                lines.get(second),
                ALREADY_BOUND.formatted(duplicate.chord().text(), first.configurationName()) + where);
    }

    private Chord chordOf(ConfigurationEntry entry) {
        ReservedAction action = ACTIONS.get(entry.key());
        if (!(entry.value() instanceof ScalarNode scalar) || scalar.getValue().isBlank()) {
            throw refused(entry.line(), NEEDS_A_CHORD.formatted(entry.key(), action.defaultChord().text()));
        }
        try {
            return ChordSpelling.configured(scalar.getValue());
        } catch (InvalidChordException invalid) {
            throw refused(entry.line(), invalid.getMessage());
        }
    }

    private boolean desktop(ConfigurationEntry entry) {
        if (!(entry.value() instanceof ScalarNode scalar)) {
            throw refused(entry.line(), NEEDS_A_BOOLEAN.formatted(entry.key()));
        }
        if (!Tag.BOOL.equals(scalar.getTag())) {
            throw refused(entry.line(), NOT_A_BOOLEAN.formatted(scalar.getValue(), entry.key()));
        }
        return Boolean.parseBoolean(scalar.getValue());
    }

    private ConfigurationRefusedException refused(int line, String reason) {
        return new ConfigurationRefusedException(LOCATED.formatted(file, line, reason));
    }

    /** The line of the code point at {@code position}, counted from 1. */
    private static int lineAt(String text, int position) {
        return FIRST_LINE + (int) text.codePoints().limit(position).filter(point -> point == NEWLINE).count();
    }

    private static int lineOf(Optional<Mark> mark) {
        return mark.map(at -> at.getLine() + FIRST_LINE).orElse(FIRST_LINE);
    }
}

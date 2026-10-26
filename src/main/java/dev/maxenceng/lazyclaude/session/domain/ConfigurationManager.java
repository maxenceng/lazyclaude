package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.published.Chord;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Reads the configuration and, once it is valid, compares its reserved chords with the ones Claude Code binds. A clash is not
 * refused, since those bindings are Claude Code's: it is one warning naming every one the user bound, and another naming every
 * default of Claude Code's. A file that cannot be read says the check of the user's bindings was skipped.
 */
public final class ConfigurationManager {

    private static final String CLASH = "keybindings.json binds %s, which lazyclaude takes before claude inside a hosted session: rebind them in config.yml.";
    private static final String DEFAULT_CLASH = "claude binds %s by default, which lazyclaude takes before claude inside a hosted session.";
    private static final String SKIPPED = "keybindings.json clash check skipped: %s.";
    private static final String CLASHING_ACTION = "%s (%s)";
    private static final String SEPARATOR = ", ";

    private final ConfigurationPort configurations;
    private final ClaudeKeybindingsPort keybindings;
    private final WarningPort warnings;

    public ConfigurationManager(ConfigurationPort configurations, ClaudeKeybindingsPort keybindings, WarningPort warnings) {
        this.configurations = configurations;
        this.keybindings = keybindings;
        this.warnings = warnings;
    }

    /** @throws ConfigurationRefusedException when the file is not valid */
    public Configuration configuration() {
        Configuration configuration = configurations.configuration();
        warnOfClashes(DEFAULT_CLASH, configuration.reservedChords(), keybindings.defaults());
        try {
            warnOfClashes(CLASH, configuration.reservedChords(), keybindings.bindings());
        } catch (ClaudeKeybindingsUnreadableException unreadable) {
            warnings.raise(new Warning(SKIPPED.formatted(unreadable.getMessage())));
        }
        return configuration;
    }

    private void warnOfClashes(String warning, ReservedChords reserved, List<Chord> bound) {
        List<ReservedAction> clashing = reserved.clashingWith(bound);
        if (!clashing.isEmpty()) {
            warnings.raise(new Warning(warning.formatted(clashing.stream()
                    .map(action -> CLASHING_ACTION.formatted(reserved.of(action).text(), action.configurationName()))
                    .collect(Collectors.joining(SEPARATOR)))));
        }
    }
}

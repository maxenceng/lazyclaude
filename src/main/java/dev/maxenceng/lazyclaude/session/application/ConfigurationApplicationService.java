package dev.maxenceng.lazyclaude.session.application;

import dev.maxenceng.lazyclaude.session.domain.ClaudeKeybindingsPort;
import dev.maxenceng.lazyclaude.session.domain.Configuration;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationManager;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/** Reads lazyclaude's configuration, and warns of the chords it reserves that Claude Code binds too. */
public final class ConfigurationApplicationService {

    private final ConfigurationManager manager;

    public ConfigurationApplicationService(
            ConfigurationPort configurations, ClaudeKeybindingsPort keybindings, WarningPort warnings) {
        this.manager = new ConfigurationManager(configurations, keybindings, warnings);
    }

    public Configuration configuration() {
        return manager.configuration();
    }
}

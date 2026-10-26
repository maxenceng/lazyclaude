package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.ConfigurationApplicationService;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationRefusedException;

/** What start-up asks of {@code session} before anything else is read: the configuration, once. */
public final class ConfigurationQuery {

    private final ConfigurationApplicationService configurations;

    public ConfigurationQuery(ConfigurationApplicationService configurations) {
        this.configurations = configurations;
    }

    /** @throws ConfigurationRefusedException when the file is not valid, naming the file, the line and the reason */
    public ConfigurationView configuration() {
        return ConfigurationView.from(configurations.configuration());
    }
}

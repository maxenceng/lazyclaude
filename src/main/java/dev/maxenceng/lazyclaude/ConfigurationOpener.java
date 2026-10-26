package dev.maxenceng.lazyclaude;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;

/** Opens the application on what the user configured; a seam so the start-up can be tested without a terminal. */
@FunctionalInterface
interface ConfigurationOpener {

    void open(ConfigurationView configuration) throws Exception;
}

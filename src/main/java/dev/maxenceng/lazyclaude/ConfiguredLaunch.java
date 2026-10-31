package dev.maxenceng.lazyclaude;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;

/** What a launch runs once the configuration is read and every context built over it; a seam so the loading is written once. */
@FunctionalInterface
interface ConfiguredLaunch {

    int run(ConfigurationView configuration, Contexts contexts) throws Exception;
}

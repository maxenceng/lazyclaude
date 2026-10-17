package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Where in the configuration a line is: its file and its line number. */
public record ConfigPlace(KittyConfigFile file, ConfigLineNumber line) {

    public ConfigPlace {
        Assert.notNull("file", file);
        Assert.notNull("line", line);
    }

    public static ConfigPlaceBuilder builder() {
        return new ConfigPlaceBuilder();
    }

    public static final class ConfigPlaceBuilder {
        private KittyConfigFile file;
        private ConfigLineNumber line;

        private ConfigPlaceBuilder() {}

        public ConfigPlaceBuilder file(KittyConfigFile file) {
            this.file = file;
            return this;
        }

        public ConfigPlaceBuilder line(ConfigLineNumber line) {
            this.line = line;
            return this;
        }

        public ConfigPlace build() {
            return new ConfigPlace(file, line);
        }
    }
}

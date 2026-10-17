package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An option the configuration sets, and the line that sets it. */
public record ConfigSetting(ConfigLine line, ConfigPlace place) {

    public ConfigSetting {
        Assert.notNull("line", line);
        Assert.notNull("place", place);
    }

    public static ConfigSettingBuilder builder() {
        return new ConfigSettingBuilder();
    }

    public static final class ConfigSettingBuilder {
        private ConfigLine line;
        private ConfigPlace place;

        private ConfigSettingBuilder() {}

        public ConfigSettingBuilder line(ConfigLine line) {
            this.line = line;
            return this;
        }

        public ConfigSettingBuilder place(ConfigPlace place) {
            this.place = place;
            return this;
        }

        public ConfigSetting build() {
            return new ConfigSetting(line, place);
        }
    }
}

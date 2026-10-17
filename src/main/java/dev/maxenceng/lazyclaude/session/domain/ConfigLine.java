package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An option set to a value, as a line of {@code kitty.conf} would say it. */
public record ConfigLine(ConfigKey key, ConfigValue value) {

    public ConfigLine {
        Assert.notNull("key", key);
        Assert.notNull("value", value);
    }

    public static ConfigLineBuilder builder() {
        return new ConfigLineBuilder();
    }

    public static final class ConfigLineBuilder {
        private ConfigKey key;
        private ConfigValue value;

        private ConfigLineBuilder() {}

        public ConfigLineBuilder key(ConfigKey key) {
            this.key = key;
            return this;
        }

        public ConfigLineBuilder value(ConfigValue value) {
            this.value = value;
            return this;
        }

        public ConfigLine build() {
            return new ConfigLine(key, value);
        }
    }
}

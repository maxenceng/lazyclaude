package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An option set to a value, as a line of {@code kitty.conf} would say it. */
public record KittyConfigLine(KittyConfigKey key, KittyConfigValue value) {

    public KittyConfigLine {
        Assert.notNull("key", key);
        Assert.notNull("value", value);
    }

    public static KittyConfigLineBuilder builder() {
        return new KittyConfigLineBuilder();
    }

    public static final class KittyConfigLineBuilder {
        private KittyConfigKey key;
        private KittyConfigValue value;

        private KittyConfigLineBuilder() {}

        public KittyConfigLineBuilder key(KittyConfigKey key) {
            this.key = key;
            return this;
        }

        public KittyConfigLineBuilder value(KittyConfigValue value) {
            this.value = value;
            return this;
        }

        public KittyConfigLine build() {
            return new KittyConfigLine(key, value);
        }
    }
}

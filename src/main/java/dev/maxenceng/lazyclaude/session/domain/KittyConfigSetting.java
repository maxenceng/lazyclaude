package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An option the configuration sets, and the line that sets it. */
public record KittyConfigSetting(KittyConfigLine line, KittyConfigPlace place) {

    public KittyConfigSetting {
        Assert.notNull("line", line);
        Assert.notNull("place", place);
    }

    public static KittyConfigSettingBuilder builder() {
        return new KittyConfigSettingBuilder();
    }

    public static final class KittyConfigSettingBuilder {
        private KittyConfigLine line;
        private KittyConfigPlace place;

        private KittyConfigSettingBuilder() {}

        public KittyConfigSettingBuilder line(KittyConfigLine line) {
            this.line = line;
            return this;
        }

        public KittyConfigSettingBuilder place(KittyConfigPlace place) {
            this.place = place;
            return this;
        }

        public KittyConfigSetting build() {
            return new KittyConfigSetting(line, place);
        }
    }
}

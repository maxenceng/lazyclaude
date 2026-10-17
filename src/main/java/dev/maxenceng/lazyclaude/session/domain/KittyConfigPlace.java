package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Where in the configuration a line is: its file and its line number. */
public record KittyConfigPlace(KittyConfigFile file, KittyConfigLineNumber line) {

    public KittyConfigPlace {
        Assert.notNull("file", file);
        Assert.notNull("line", line);
    }

    public static KittyConfigPlaceBuilder builder() {
        return new KittyConfigPlaceBuilder();
    }

    public static final class KittyConfigPlaceBuilder {
        private KittyConfigFile file;
        private KittyConfigLineNumber line;

        private KittyConfigPlaceBuilder() {}

        public KittyConfigPlaceBuilder file(KittyConfigFile file) {
            this.file = file;
            return this;
        }

        public KittyConfigPlaceBuilder line(KittyConfigLineNumber line) {
            this.line = line;
            return this;
        }

        public KittyConfigPlace build() {
            return new KittyConfigPlace(file, line);
        }
    }
}

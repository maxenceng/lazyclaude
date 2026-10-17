package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** An option is already set to another value than the one wanted, by the line {@code found}, in the file that line is in. */
public record ConflictingRemoteControl(KittyConfigSetting found, KittyConfigLine wanted) implements RemoteControlConfig {

    public ConflictingRemoteControl {
        Assert.notNull("found", found);
        Assert.notNull("wanted", wanted);
    }

    public static ConflictingRemoteControlBuilder builder() {
        return new ConflictingRemoteControlBuilder();
    }

    public static final class ConflictingRemoteControlBuilder {
        private KittyConfigSetting found;
        private KittyConfigLine wanted;

        private ConflictingRemoteControlBuilder() {}

        public ConflictingRemoteControlBuilder found(KittyConfigSetting found) {
            this.found = found;
            return this;
        }

        public ConflictingRemoteControlBuilder wanted(KittyConfigLine wanted) {
            this.wanted = wanted;
            return this;
        }

        public ConflictingRemoteControl build() {
            return new ConflictingRemoteControl(found, wanted);
        }
    }
}

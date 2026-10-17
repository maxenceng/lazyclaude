package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Neither option conflicts, and these lines, the ones not yet set, can be appended to the file. */
public record AddRemoteControl(KittyConfigFile file, List<ConfigLine> lines) implements RemoteControlConfig {

    public AddRemoteControl {
        Assert.notNull("file", file);
        Assert.notEmpty("lines", lines);
    }

    public static AddRemoteControlBuilder builder() {
        return new AddRemoteControlBuilder();
    }

    public static final class AddRemoteControlBuilder {
        private KittyConfigFile file;
        private List<ConfigLine> lines;

        private AddRemoteControlBuilder() {}

        public AddRemoteControlBuilder file(KittyConfigFile file) {
            this.file = file;
            return this;
        }

        public AddRemoteControlBuilder lines(List<ConfigLine> lines) {
            this.lines = lines;
            return this;
        }

        public AddRemoteControl build() {
            return new AddRemoteControl(file, lines);
        }
    }
}

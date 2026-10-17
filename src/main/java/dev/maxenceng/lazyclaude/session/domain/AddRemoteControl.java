package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Neither option conflicts, and these lines, the ones not yet set, can be appended to the file. */
public record AddRemoteControl(KittyConfigFile file, List<KittyConfigLine> lines) implements RemoteControlConfig {

    private static final String ADDED_BY = "# added by lazyclaude";
    private static final String LINE = "%s %s";

    public AddRemoteControl {
        Assert.notNull("file", file);
        Assert.notEmpty("lines", lines);
    }

    /** The text appended to the file, line by line: a comment saying lazyclaude added them, then each line as kitty reads it. */
    public List<String> text() {
        return Stream.concat(
                        Stream.of(ADDED_BY),
                        lines.stream().map(line -> LINE.formatted(line.key().value(), line.value().value())))
                .toList();
    }

    public static AddRemoteControlBuilder builder() {
        return new AddRemoteControlBuilder();
    }

    public static final class AddRemoteControlBuilder {
        private KittyConfigFile file;
        private List<KittyConfigLine> lines;

        private AddRemoteControlBuilder() {}

        public AddRemoteControlBuilder file(KittyConfigFile file) {
            this.file = file;
            return this;
        }

        public AddRemoteControlBuilder lines(List<KittyConfigLine> lines) {
            this.lines = lines;
            return this;
        }

        public AddRemoteControl build() {
            return new AddRemoteControl(file, lines);
        }
    }
}

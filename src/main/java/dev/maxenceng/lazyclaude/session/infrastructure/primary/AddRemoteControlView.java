package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.AddRemoteControl;

/** The file the lines would be appended to, and the lines as they would read in it. */
public record AddRemoteControlView(Path file, List<String> lines) implements RemoteControlConfigView {

    private static final String LINE = "%s %s";

    static AddRemoteControlView from(AddRemoteControl adding) {
        return builder()
                .file(adding.file().path())
                .lines(adding.lines().stream()
                        .map(line -> LINE.formatted(line.key().value(), line.value().value()))
                        .toList())
                .build();
    }

    public static AddRemoteControlViewBuilder builder() {
        return new AddRemoteControlViewBuilder();
    }

    public static final class AddRemoteControlViewBuilder {
        private Path file;
        private List<String> lines;

        private AddRemoteControlViewBuilder() {}

        public AddRemoteControlViewBuilder file(Path file) {
            this.file = file;
            return this;
        }

        public AddRemoteControlViewBuilder lines(List<String> lines) {
            this.lines = lines;
            return this;
        }

        public AddRemoteControlView build() {
            return new AddRemoteControlView(file, lines);
        }
    }
}

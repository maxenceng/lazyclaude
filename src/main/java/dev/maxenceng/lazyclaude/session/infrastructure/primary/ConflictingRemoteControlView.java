package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.session.domain.ConflictingRemoteControl;

/** The line that sets a key another way than remote control needs: its file, its line number, the line as it reads and the line that was wanted. */
public record ConflictingRemoteControlView(Path file, int line, String setting, String wanted)
        implements RemoteControlConfigView {

    private static final String LINE = "%s %s";

    static ConflictingRemoteControlView from(ConflictingRemoteControl conflicting) {
        return builder()
                .file(conflicting.found().place().file().path())
                .line(conflicting.found().place().line().value())
                .setting(LINE.formatted(
                        conflicting.found().line().key().value(),
                        conflicting.found().line().value().value()))
                .wanted(LINE.formatted(conflicting.wanted().key().value(), conflicting.wanted().value().value()))
                .build();
    }

    public static ConflictingRemoteControlViewBuilder builder() {
        return new ConflictingRemoteControlViewBuilder();
    }

    public static final class ConflictingRemoteControlViewBuilder {
        private Path file;
        private int line;
        private String setting;
        private String wanted;

        private ConflictingRemoteControlViewBuilder() {}

        public ConflictingRemoteControlViewBuilder file(Path file) {
            this.file = file;
            return this;
        }

        public ConflictingRemoteControlViewBuilder line(int line) {
            this.line = line;
            return this;
        }

        public ConflictingRemoteControlViewBuilder setting(String setting) {
            this.setting = setting;
            return this;
        }

        public ConflictingRemoteControlViewBuilder wanted(String wanted) {
            this.wanted = wanted;
            return this;
        }

        public ConflictingRemoteControlView build() {
            return new ConflictingRemoteControlView(file, line, setting, wanted);
        }
    }
}

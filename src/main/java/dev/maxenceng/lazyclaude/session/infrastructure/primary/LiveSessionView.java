package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.time.LocalDateTime;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.LiveSession;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;

/** One live session as the shell reads it: what its row and the main view show. */
public record LiveSessionView(
        SessionId id,
        ProcessId pid,
        SessionName name,
        Path directory,
        LocalDateTime startedAt,
        SessionAge age,
        ClaudeVersion version,
        SessionState state) {

    static LiveSessionView from(AvailableSessions available, LiveSession session) {
        return builder()
                .id(session.id())
                .pid(session.pid())
                .name(session.name())
                .directory(session.directory().path())
                .startedAt(available.startedLocally(session))
                .age(available.ageOf(session))
                .version(session.version())
                .state(session.state())
                .build();
    }

    public static LiveSessionViewBuilder builder() {
        return new LiveSessionViewBuilder();
    }

    public static final class LiveSessionViewBuilder {
        private SessionId id;
        private ProcessId pid;
        private SessionName name;
        private Path directory;
        private LocalDateTime startedAt;
        private SessionAge age;
        private ClaudeVersion version;
        private SessionState state;

        private LiveSessionViewBuilder() {}

        public LiveSessionViewBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public LiveSessionViewBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public LiveSessionViewBuilder name(SessionName name) {
            this.name = name;
            return this;
        }

        public LiveSessionViewBuilder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public LiveSessionViewBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public LiveSessionViewBuilder age(SessionAge age) {
            this.age = age;
            return this;
        }

        public LiveSessionViewBuilder version(ClaudeVersion version) {
            this.version = version;
            return this;
        }

        public LiveSessionViewBuilder state(SessionState state) {
            this.state = state;
            return this;
        }

        public LiveSessionView build() {
            return new LiveSessionView(id, pid, name, directory, startedAt, age, version, state);
        }
    }
}

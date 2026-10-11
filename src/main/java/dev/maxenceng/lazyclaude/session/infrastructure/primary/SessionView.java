package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.time.LocalDateTime;

import dev.maxenceng.lazyclaude.session.domain.LiveSession;
import dev.maxenceng.lazyclaude.session.domain.LiveSessions;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;

/** One live session as the shell reads it: what its row and the main view show. */
public record SessionView(
        SessionId id,
        ProcessId pid,
        SessionName name,
        Path directory,
        LocalDateTime startedAt,
        SessionAge age,
        ClaudeVersion version,
        SessionState state) {

    static SessionView from(LiveSessions live, LiveSession session) {
        return builder()
                .id(session.id())
                .pid(session.pid())
                .name(session.name())
                .directory(session.directory().path())
                .startedAt(live.startedLocally(session))
                .age(live.ageOf(session))
                .version(session.version())
                .state(session.state())
                .build();
    }

    public static SessionViewBuilder builder() {
        return new SessionViewBuilder();
    }

    public static final class SessionViewBuilder {
        private SessionId id;
        private ProcessId pid;
        private SessionName name;
        private Path directory;
        private LocalDateTime startedAt;
        private SessionAge age;
        private ClaudeVersion version;
        private SessionState state;

        private SessionViewBuilder() {}

        public SessionViewBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public SessionViewBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public SessionViewBuilder name(SessionName name) {
            this.name = name;
            return this;
        }

        public SessionViewBuilder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public SessionViewBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public SessionViewBuilder age(SessionAge age) {
            this.age = age;
            return this;
        }

        public SessionViewBuilder version(ClaudeVersion version) {
            this.version = version;
            return this;
        }

        public SessionViewBuilder state(SessionState state) {
            this.state = state;
            return this;
        }

        public SessionView build() {
            return new SessionView(id, pid, name, directory, startedAt, age, version, state);
        }
    }
}

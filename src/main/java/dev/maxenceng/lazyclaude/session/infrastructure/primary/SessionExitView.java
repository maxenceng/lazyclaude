package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** A hosted session whose {@code claude} exited by itself, and the code it exited with; zero is a clean exit. */
public record SessionExitView(SessionId session, int code) {

    public static SessionExitViewBuilder builder() {
        return new SessionExitViewBuilder();
    }

    public static final class SessionExitViewBuilder {
        private SessionId session;
        private int code;

        private SessionExitViewBuilder() {}

        public SessionExitViewBuilder session(SessionId session) {
            this.session = session;
            return this;
        }

        public SessionExitViewBuilder code(int code) {
            this.code = code;
            return this;
        }

        public SessionExitView build() {
            return new SessionExitView(session, code);
        }
    }
}

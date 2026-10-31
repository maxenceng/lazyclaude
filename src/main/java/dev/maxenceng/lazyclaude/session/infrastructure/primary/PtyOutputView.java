package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What a hosted session's {@code claude} wrote to its pty in one read, as it wrote it, for the frontend that emulates it. */
public record PtyOutputView(SessionId session, PtyBytes data) {

    public static PtyOutputViewBuilder builder() {
        return new PtyOutputViewBuilder();
    }

    public static final class PtyOutputViewBuilder {
        private SessionId session;
        private PtyBytes data;

        private PtyOutputViewBuilder() {}

        public PtyOutputViewBuilder session(SessionId session) {
            this.session = session;
            return this;
        }

        public PtyOutputViewBuilder data(PtyBytes data) {
            this.data = data;
            return this;
        }

        public PtyOutputView build() {
            return new PtyOutputView(session, data);
        }
    }
}

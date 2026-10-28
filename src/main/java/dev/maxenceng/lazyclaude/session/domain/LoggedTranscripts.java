package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Map;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * What the transcripts of the logged sessions say, by session id, and which of those sessions have no transcript any more. A
 * logged session in neither is not read yet.
 */
public record LoggedTranscripts(Map<SessionId, TranscriptFacts> read, List<SessionId> gone) {

    public LoggedTranscripts {
        Assert.notNull("read transcripts", read);
        Assert.notNull("gone transcripts", gone);
    }

    public static LoggedTranscriptsBuilder builder() {
        return new LoggedTranscriptsBuilder();
    }

    public static final class LoggedTranscriptsBuilder {
        private Map<SessionId, TranscriptFacts> read = Map.of();
        private List<SessionId> gone = List.of();

        private LoggedTranscriptsBuilder() {}

        public LoggedTranscriptsBuilder read(Map<SessionId, TranscriptFacts> read) {
            this.read = read;
            return this;
        }

        public LoggedTranscriptsBuilder gone(List<SessionId> gone) {
            this.gone = gone;
            return this;
        }

        public LoggedTranscripts build() {
            return new LoggedTranscripts(read, gone);
        }
    }
}

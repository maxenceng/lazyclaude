package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * What the transcripts of the logged sessions say, and which of those sessions have no transcript any more. A logged session in
 * neither is not read yet.
 */
public record LoggedTranscripts(List<TranscriptFacts> read, List<SessionId> gone) {

    public LoggedTranscripts {
        Assert.notNull("read transcripts", read);
        Assert.notNull("gone transcripts", gone);
    }

    /** What the transcript of {@code id} says, the first one read when two folders hold it; empty when it is not read. */
    public Optional<TranscriptFacts> of(SessionId id) {
        return read.stream().filter(facts -> facts.id().equals(id)).findFirst();
    }

    public static LoggedTranscriptsBuilder builder() {
        return new LoggedTranscriptsBuilder();
    }

    public static final class LoggedTranscriptsBuilder {
        private List<TranscriptFacts> read = List.of();
        private List<SessionId> gone = List.of();

        private LoggedTranscriptsBuilder() {}

        public LoggedTranscriptsBuilder read(List<TranscriptFacts> read) {
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

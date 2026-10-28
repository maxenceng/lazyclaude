package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.SessionRecordUnreadableException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Reads the JSON Claude Code writes into a session; the one place Jackson is used for it. */
final class SessionRecords {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private SessionRecords() {}

    /** @throws SessionRecordUnreadableException saying what {@code source} held that could not be read */
    static SessionRecord record(byte[] json, String source) {
        SessionRecord read;
        try {
            read = MAPPER.readValue(json, SessionRecord.class);
        } catch (JacksonException e) {
            throw new SessionRecordUnreadableException("%s does not parse: %s".formatted(source, e.getOriginalMessage()), e);
        }
        if (read == null) {
            throw new SessionRecordUnreadableException("%s is not a session record: it holds null".formatted(source));
        }
        return read;
    }

    /**
     * None while the record has no status yet.
     *
     * @throws SessionRecordUnreadableException saying what {@code source} left out of the record
     */
    static Optional<ReportedSession> session(SessionRecord record, String source) {
        if (record.missingFields().any()) {
            throw new SessionRecordUnreadableException(
                    "%s is not a session record: it has no %s".formatted(source, record.missingFields().listed()));
        }
        return record.hasStatus() ? Optional.of(record.toReportedSession()) : Optional.empty();
    }
}

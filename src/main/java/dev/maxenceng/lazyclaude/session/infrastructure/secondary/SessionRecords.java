package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Reads the JSON Claude Code writes into sessions; the one place Jackson is used for them. */
final class SessionRecords {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private SessionRecords() {}

    /** @throws SessionsUnavailableException saying what {@code source} held that could not be read */
    static SessionRecord record(byte[] json, String source) {
        try {
            return MAPPER.readValue(json, SessionRecord.class);
        } catch (JacksonException e) {
            throw new SessionsUnavailableException("%s does not parse: %s".formatted(source, e.getOriginalMessage()), e);
        }
    }

    /** @throws SessionsUnavailableException saying what {@code source} held that could not be read */
    static List<SessionRecord> records(byte[] json, String source) {
        try {
            return Arrays.asList(MAPPER.readValue(json, SessionRecord[].class));
        } catch (JacksonException e) {
            throw new SessionsUnavailableException("%s does not parse: %s".formatted(source, e.getOriginalMessage()), e);
        }
    }

    /** @throws SessionsUnavailableException saying what {@code source} left out of the record, or that its directory is not a path */
    static ReportedSession session(SessionRecord record, String source) {
        if (record.missingFields().any()) {
            throw new SessionsUnavailableException(
                    "%s is not a session record: it has no %s".formatted(source, record.missingFields().listed()));
        }
        try {
            return record.toReportedSession();
        } catch (InvalidPathException refused) {
            throw new SessionsUnavailableException("%s is not a session record: %s".formatted(source, refused.getMessage()), refused);
        }
    }
}

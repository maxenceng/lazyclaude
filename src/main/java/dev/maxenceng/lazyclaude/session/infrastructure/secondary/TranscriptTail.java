package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What a run of transcript lines adds to what is already known: the last {@code cwd}, the last title, the first prompt and
 * first timestamp when none is known yet, and the last timestamp. A line that cannot carry any of them is not parsed, and
 * one that does not parse is skipped, so the rest of the transcript still counts.
 */
final class TranscriptTail {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String TIMESTAMP_KEY = "\"timestamp\"";
    private static final String AI_TITLE_KEY = "\"aiTitle\"";
    private static final String USER = "user";
    private static final String CONTENT = "content";
    private static final String TEXT = "text";

    private String cwd;
    private String aiTitle;
    private String firstPrompt;
    private Long startedAt;
    private Long lastActivity;

    TranscriptTail(CachedTranscript known) {
        this.cwd = known.cwd();
        this.aiTitle = known.aiTitle();
        this.firstPrompt = known.firstPrompt();
        this.startedAt = known.startedAt();
        this.lastActivity = known.lastActivity();
    }

    void accept(byte[] line) {
        String text = new String(line, StandardCharsets.UTF_8);
        if (!text.contains(TIMESTAMP_KEY) && !text.contains(AI_TITLE_KEY)) {
            return;
        }
        TranscriptLine entry;
        try {
            entry = MAPPER.readValue(text, TranscriptLine.class);
        } catch (JacksonException notJson) {
            return;
        }
        if (entry == null) {
            return;
        }
        remember(entry);
    }

    private void remember(TranscriptLine entry) {
        if (entry.cwd() != null && !entry.cwd().isBlank()) {
            cwd = entry.cwd();
        }
        if (entry.aiTitle() != null && !entry.aiTitle().isBlank()) {
            aiTitle = entry.aiTitle();
        }
        instantOf(entry.timestamp()).ifPresent(this::seen);
        if (firstPrompt == null && USER.equals(entry.type()) && !Boolean.TRUE.equals(entry.isMeta())) {
            firstPrompt = promptOf(entry.message()).orElse(null);
        }
    }

    private void seen(long millis) {
        if (startedAt == null) {
            startedAt = millis;
        }
        lastActivity = millis;
    }

    private static Optional<Long> instantOf(String timestamp) {
        if (timestamp == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(timestamp).toEpochMilli());
        } catch (DateTimeParseException | ArithmeticException notATimestamp) {
            return Optional.empty();
        }
    }

    private static Optional<String> promptOf(JsonNode message) {
        if (message == null) {
            return Optional.empty();
        }
        JsonNode content = message.get(CONTENT);
        if (content == null) {
            return Optional.empty();
        }
        if (content.isString()) {
            return nonBlank(content.asString());
        }
        for (JsonNode block : content) {
            JsonNode text = block.get(TEXT);
            if (text != null && text.isString() && nonBlank(text.asString()).isPresent()) {
                return Optional.of(text.asString());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> nonBlank(String text) {
        return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    /** {@code known} with everything the lines added, at the file as it now is. */
    CachedTranscript into(CachedTranscript known, long size, long modified, String fileKey, long offset) {
        return new CachedTranscript(
                known.path(),
                size,
                modified,
                fileKey,
                offset,
                known.sessionId(),
                cwd,
                known.projectRoot(),
                aiTitle,
                firstPrompt,
                startedAt,
                lastActivity);
    }
}

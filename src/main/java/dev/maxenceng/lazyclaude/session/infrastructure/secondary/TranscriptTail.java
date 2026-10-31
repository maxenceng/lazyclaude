package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What a run of transcript lines adds to what is already known: the last title, the first prompt when none is known yet, the
 * last timestamp, and every {@code /ticket} run, from a command's args or a {@code Skill} call, the latest of a repeated one last. A line that cannot carry any of them is not parsed, and
 * one that does not parse is skipped, so the rest of the transcript still counts.
 */
final class TranscriptTail {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String TIMESTAMP_KEY = "\"timestamp\"";
    private static final String AI_TITLE_KEY = "\"aiTitle\"";
    private static final String USER = "user";
    private static final String ASSISTANT = "assistant";
    private static final String TOOL_USE = "tool_use";
    private static final String SKILL = "Skill";
    private static final String INPUT = "input";
    private static final String ARGS = "args";
    private static final String SKILL_KEY = "skill";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final Pattern TICKET_SKILL = Pattern.compile("(?:.+:)?ticket");
    private static final Pattern TICKET_COMMAND = Pattern.compile("<command-name>/(?:[^:<]+:)?ticket</command-name>");
    private static final Pattern COMMAND_ARGS = Pattern.compile("<command-args>(.*?)</command-args>", Pattern.DOTALL);
    private static final String CONTENT = "content";
    private static final String TEXT = "text";

    private String aiTitle;
    private String firstPrompt;
    private Long lastActivity;
    private List<CachedTicketRun> ticketRuns;

    TranscriptTail(CachedTranscript known) {
        this.aiTitle = known.aiTitle();
        this.firstPrompt = known.firstPrompt();
        this.lastActivity = known.lastActivity();
        this.ticketRuns = known.ticketRuns();
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
        if (entry.aiTitle() != null && !entry.aiTitle().isBlank()) {
            aiTitle = entry.aiTitle();
        }
        instantOf(entry.timestamp()).ifPresent(this::seen);
        if (firstPrompt == null && USER.equals(entry.type()) && !Boolean.TRUE.equals(entry.isMeta())) {
            firstPrompt = promptOf(entry.message()).orElse(null);
        }
        ticketRunOf(entry).ifPresent(this::ran);
    }

    private void ran(CachedTicketRun run) {
        ticketRuns = Stream.concat(ticketRuns.stream().filter(held -> !held.equals(run)), Stream.of(run)).toList();
    }

    private static Optional<CachedTicketRun> ticketRunOf(TranscriptLine entry) {
        if (entry.message() == null || entry.message().get(CONTENT) == null) {
            return Optional.empty();
        }
        JsonNode content = entry.message().get(CONTENT);
        if (USER.equals(entry.type()) && content.isString()) {
            return commandArgsOf(content.asString()).flatMap(CachedTicketRun::parse);
        }
        if (ASSISTANT.equals(entry.type())) {
            return content.valueStream()
                    .flatMap(block -> skillArgsOf(block).flatMap(CachedTicketRun::parse).stream())
                    .findFirst();
        }
        return Optional.empty();
    }

    private static Optional<String> commandArgsOf(String content) {
        if (!TICKET_COMMAND.matcher(content).find()) {
            return Optional.empty();
        }
        Matcher args = COMMAND_ARGS.matcher(content);
        return args.find() ? Optional.of(args.group(1)) : Optional.empty();
    }

    private static Optional<String> skillArgsOf(JsonNode block) {
        JsonNode input = block.get(INPUT);
        if (input == null || isNotText(block.get(TYPE), TOOL_USE) || isNotText(block.get(NAME), SKILL)) {
            return Optional.empty();
        }
        JsonNode skill = input.get(SKILL_KEY);
        JsonNode args = input.get(ARGS);
        boolean ticket = skill != null && skill.isString() && TICKET_SKILL.matcher(skill.asString()).matches();
        return ticket && args != null && args.isString() ? Optional.of(args.asString()) : Optional.empty();
    }

    private static boolean isNotText(JsonNode node, String expected) {
        return node == null || !node.isString() || !expected.equals(node.asString());
    }

    private void seen(long millis) {
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
        return content.valueStream()
                .map(block -> block.get(TEXT))
                .filter(text -> text != null && text.isString())
                .flatMap(text -> nonBlank(text.asString()).stream())
                .findFirst();
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
                aiTitle,
                firstPrompt,
                lastActivity,
                ticketRuns);
    }
}

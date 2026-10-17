package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.published.EntryText;
import dev.maxenceng.lazyclaude.session.published.NoToolInput;
import dev.maxenceng.lazyclaude.session.published.PromptEntry;
import dev.maxenceng.lazyclaude.session.published.ReplyEntry;
import dev.maxenceng.lazyclaude.session.published.ToolArgument;
import dev.maxenceng.lazyclaude.session.published.ToolCallEntry;
import dev.maxenceng.lazyclaude.session.published.ToolInput;
import dev.maxenceng.lazyclaude.session.published.ToolName;
import dev.maxenceng.lazyclaude.session.published.TranscriptEntry;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What one transcript line shows: the user's prompts (text, never a tool result or a meta prompt), Claude's replies (text,
 * never thinking) and one entry per tool call. A line that does not parse shows nothing, so the rest of the transcript still does.
 */
final class TranscriptEntryLine {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String USER = "user";
    private static final String ASSISTANT = "assistant";
    private static final String CONTENT = "content";
    private static final String TYPE = "type";
    private static final String TEXT = "text";
    private static final String TOOL_USE = "tool_use";
    private static final String NAME = "name";
    private static final String INPUT = "input";
    private static final List<String> SAYING_WHAT_A_TOOL_DOES =
            List.of("command", "file_path", "pattern", "path", "url", "query", "description", "skill", "prompt");
    private static final String WHITESPACE = "\\s+";
    private static final String ONE_SPACE = " ";

    private TranscriptEntryLine() {}

    static Stream<TranscriptEntry> entriesIn(byte[] line) {
        TranscriptLine entry;
        try {
            entry = MAPPER.readValue(new String(line, StandardCharsets.UTF_8), TranscriptLine.class);
        } catch (JacksonException notJson) {
            return Stream.empty();
        }
        if (entry == null || entry.message() == null || entry.message().get(CONTENT) == null) {
            return Stream.empty();
        }
        JsonNode content = entry.message().get(CONTENT);
        if (USER.equals(entry.type()) && !Boolean.TRUE.equals(entry.isMeta())) {
            return prompts(content);
        }
        return ASSISTANT.equals(entry.type()) ? answers(content) : Stream.empty();
    }

    private static Stream<TranscriptEntry> prompts(JsonNode content) {
        if (content.isString()) {
            return text(content.asString()).<TranscriptEntry>map(PromptEntry::new).stream();
        }
        return content.valueStream()
                .filter(block -> isText(block.get(TYPE), TEXT))
                .flatMap(block -> text(block.get(TEXT)).<TranscriptEntry>map(PromptEntry::new).stream());
    }

    private static Stream<TranscriptEntry> answers(JsonNode content) {
        return content.valueStream().flatMap(block -> {
            if (isText(block.get(TYPE), TEXT)) {
                return text(block.get(TEXT)).<TranscriptEntry>map(ReplyEntry::new).stream();
            }
            return isText(block.get(TYPE), TOOL_USE) ? toolCall(block).stream() : Stream.<TranscriptEntry>empty();
        });
    }

    private static Optional<TranscriptEntry> toolCall(JsonNode block) {
        return Optional.ofNullable(block.get(NAME))
                .filter(JsonNode::isString)
                .map(JsonNode::asString)
                .filter(name -> !name.isBlank())
                .map(name -> ToolCallEntry.builder()
                        .tool(new ToolName(name))
                        .argument(argumentOf(block.get(INPUT)))
                        .build());
    }

    /** The first input that says what the tool does, else the first text the input holds, on one line. */
    private static ToolArgument argumentOf(JsonNode input) {
        if (input == null) {
            return new NoToolInput();
        }
        return SAYING_WHAT_A_TOOL_DOES.stream()
                .map(input::get)
                .flatMap(value -> oneLine(value).stream())
                .findFirst()
                .or(() -> input.properties().stream()
                        .map(Map.Entry::getValue)
                        .flatMap(value -> oneLine(value).stream())
                        .findFirst())
                .<ToolArgument>map(ToolInput::new)
                .orElseGet(NoToolInput::new);
    }

    private static Optional<String> oneLine(JsonNode value) {
        return Optional.ofNullable(value)
                .filter(JsonNode::isString)
                .map(node -> node.asString().replaceAll(WHITESPACE, ONE_SPACE).strip())
                .filter(line -> !line.isEmpty());
    }

    private static Optional<EntryText> text(JsonNode node) {
        return Optional.ofNullable(node).filter(JsonNode::isString).flatMap(found -> text(found.asString()));
    }

    private static Optional<EntryText> text(String text) {
        return text == null || text.isBlank() ? Optional.empty() : Optional.of(new EntryText(text.strip()));
    }

    private static boolean isText(JsonNode node, String expected) {
        return node != null && node.isString() && expected.equals(node.asString());
    }
}

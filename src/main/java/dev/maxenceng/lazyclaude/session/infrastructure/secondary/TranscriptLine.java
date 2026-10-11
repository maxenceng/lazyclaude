package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import tools.jackson.databind.JsonNode;

/**
 * One line of a transcript as Claude Code writes it, reduced to the fields lazyclaude reads: every one is as the JSON
 * has it, null where the JSON lacks it. {@code aiTitle} is on the {@code ai-title} entries, which carry no
 * {@code timestamp}; {@code message} is on the {@code user} and {@code assistant} ones.
 */
record TranscriptLine(String type, String timestamp, String cwd, String aiTitle, Boolean isMeta, JsonNode message) {}

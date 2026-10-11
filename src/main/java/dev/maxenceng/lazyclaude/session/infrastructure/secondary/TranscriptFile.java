package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;

/**
 * What makes a file a transcript: a {@code <sessionId>.jsonl} directly in a folder of {@code ~/.claude/projects/}. A
 * {@code subagents/} transcript sits a level deeper, so the adapters that only look that deep never meet one.
 */
final class TranscriptFile {

    private static final String SUFFIX = ".jsonl";

    private TranscriptFile() {}

    static boolean isTranscript(Path file) {
        return file.getFileName().toString().endsWith(SUFFIX);
    }

    static String sessionIdOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - SUFFIX.length());
    }
}

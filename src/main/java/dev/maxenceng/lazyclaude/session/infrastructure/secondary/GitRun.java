package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;

/** What a finished {@code git} run printed, and how it ended. */
record GitRun(int exitValue, byte[] output, byte[] said) {

    private static final int SUCCESS = 0;

    boolean succeeded() {
        return exitValue == SUCCESS;
    }

    String outputText() {
        return new String(output, StandardCharsets.UTF_8);
    }

    /** What git said on its error stream, its last line, where git puts the failure, or nothing when it said nothing. */
    String reason() {
        return new String(said, StandardCharsets.UTF_8)
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((first, last) -> last)
                .orElse("");
    }
}

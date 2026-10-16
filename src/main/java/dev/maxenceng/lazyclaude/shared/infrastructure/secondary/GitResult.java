package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.nio.charset.StandardCharsets;

/** What a finished {@code git} run printed, and how it ended. */
public record GitResult(int exitValue, byte[] output, byte[] said) {

    private static final int SUCCESS = 0;

    public boolean succeeded() {
        return exitValue == SUCCESS;
    }

    public String outputText() {
        return new String(output, StandardCharsets.UTF_8);
    }

    /** What git said on its error stream, its last line, where it puts the failure, or nothing when it said nothing. */
    public String lastLine() {
        return new String(said, StandardCharsets.UTF_8)
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((first, last) -> last)
                .orElse("");
    }

    /** What git said on its error stream, as {@code ": <first line>"}, or nothing when it said nothing. */
    public String reason() {
        return new String(said, StandardCharsets.UTF_8)
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .map(": "::concat)
                .orElse("");
    }
}

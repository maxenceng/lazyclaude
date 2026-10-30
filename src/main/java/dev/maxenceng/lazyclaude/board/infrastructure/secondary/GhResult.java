package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.charset.StandardCharsets;

/** What a finished {@code gh} run printed, and how it ended. */
record GhResult(int exitValue, byte[] output, byte[] said) {

    private static final int SUCCESS = 0;

    boolean succeeded() {
        return exitValue == SUCCESS;
    }

    String outputText() {
        return new String(output, StandardCharsets.UTF_8);
    }

    String saidText() {
        return new String(said, StandardCharsets.UTF_8);
    }

    /** What {@code gh} said on its error stream, its first line, where it puts the failure. */
    String firstLineSaid() {
        return saidText().lines().map(String::strip).filter(line -> !line.isEmpty()).findFirst().orElse(String.valueOf(exitValue));
    }
}

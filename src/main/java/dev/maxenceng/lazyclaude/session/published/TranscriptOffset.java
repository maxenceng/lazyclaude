package dev.maxenceng.lazyclaude.session.published;

/** How many bytes of a transcript have been read, which is where the next read of it starts. */
public record TranscriptOffset(long value) {

    /** Before the first byte. */
    public static TranscriptOffset start() {
        return new TranscriptOffset(0);
    }
}

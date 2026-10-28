package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;

public final class TranscriptReadingFixture {

    private TranscriptReadingFixture() {}

    public static TranscriptReading readable() {
        return new ReadableTranscript();
    }

    public static TranscriptReading unreadable() {
        return new UnreadableTranscript(reason());
    }
}

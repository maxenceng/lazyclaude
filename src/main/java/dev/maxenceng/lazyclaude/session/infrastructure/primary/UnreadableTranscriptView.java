package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** The transcripts could not be read, for the reason. */
public record UnreadableTranscriptView(Reason reason) implements TranscriptView {}

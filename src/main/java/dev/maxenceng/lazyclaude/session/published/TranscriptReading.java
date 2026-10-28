package dev.maxenceng.lazyclaude.session.published;

/** Whether a past session's transcript could be read the last time it changed; what was read of it before is kept either way. */
public sealed interface TranscriptReading permits ReadableTranscript, UnreadableTranscript {}

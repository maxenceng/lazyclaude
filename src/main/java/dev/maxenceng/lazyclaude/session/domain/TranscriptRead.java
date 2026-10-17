package dev.maxenceng.lazyclaude.session.domain;

/** What reading a session's transcript from an offset found: entries, no transcript yet, or why it could not be read. */
public sealed interface TranscriptRead permits ReadEntries, NoTranscript, UnreadableTranscript {}

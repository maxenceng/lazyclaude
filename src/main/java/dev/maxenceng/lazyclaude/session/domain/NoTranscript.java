package dev.maxenceng.lazyclaude.session.domain;

/** The session has no transcript yet, as a session just started has not; one appears with its first entry. */
public record NoTranscript() implements TranscriptRead {}

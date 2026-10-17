package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The session whose transcript the main view shows, and the directory it runs or ran in, which finds the transcript. */
record TranscriptSubject(SessionId id, Path directory) {}

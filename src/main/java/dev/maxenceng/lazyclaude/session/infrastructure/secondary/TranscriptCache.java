package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.List;

/** The cache file as JSON: the format version it was written in, and the transcripts it knows. */
record TranscriptCache(Integer version, List<CachedTranscript> transcripts) {}

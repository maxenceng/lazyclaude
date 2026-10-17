package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.NoTranscript;
import dev.maxenceng.lazyclaude.session.domain.ReadEntries;
import dev.maxenceng.lazyclaude.session.domain.TranscriptRead;
import dev.maxenceng.lazyclaude.session.domain.UnreadableTranscript;

/** What a session's transcript shows from an offset, as the shell reads it: entries, none yet, or why they cannot be read. */
public sealed interface TranscriptView permits ReadTranscriptView, NoTranscriptView, UnreadableTranscriptView {

    static TranscriptView from(TranscriptRead read) {
        return switch (read) {
            case ReadEntries entries -> ReadTranscriptView.from(entries);
            case NoTranscript none -> new NoTranscriptView();
            case UnreadableTranscript unreadable -> new UnreadableTranscriptView(unreadable.reason());
        };
    }
}

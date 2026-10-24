package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoTranscriptView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ReadTranscriptView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableTranscriptView;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TranscriptEntry;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;

/**
 * What the main view knows of the transcript it shows: whose it is, the entries read so far, and where the last read ended, which the
 * next read starts from. A read that starts where the last one ended adds to the entries; one that starts anywhere else, as a
 * transcript that was replaced does, takes their place. A read for a session no longer shown, or begun from an offset the pane
 * no longer holds, as when the session was selected again meanwhile, is dropped.
 */
final class TranscriptPane {

    private Optional<SessionId> subject = Optional.empty();
    private List<TranscriptEntry> entries = List.of();
    private TranscriptOffset offset = TranscriptOffset.start();
    private Optional<String> notice = Optional.empty();

    boolean shows(SessionId session) {
        return subject.filter(session::equals).isPresent();
    }

    /** From now on {@code session}'s transcript is the one shown, read from its start. */
    void show(SessionId session) {
        subject = Optional.of(session);
        entries = List.of();
        offset = TranscriptOffset.start();
        notice = Optional.empty();
    }

    TranscriptOffset offset() {
        return offset;
    }

    void apply(SessionId session, TranscriptOffset from, TranscriptView read) {
        if (!shows(session) || !offset.equals(from)) {
            return;
        }
        switch (read) {
            case ReadTranscriptView entriesRead -> {
                entries = entriesRead.start().equals(offset)
                        ? Stream.concat(entries.stream(), entriesRead.entries().stream()).toList()
                        : entriesRead.entries();
                offset = entriesRead.next();
                notice = Optional.empty();
            }
            case NoTranscriptView _ -> {
                entries = List.of();
                offset = TranscriptOffset.start();
                notice = Optional.of(SessionText.noTranscriptNotice());
            }
            case UnreadableTranscriptView unreadable -> {
                entries = List.of();
                offset = TranscriptOffset.start();
                notice = Optional.of(SessionText.unreadableTranscriptNotice(unreadable.reason()));
            }
        }
    }

    /** The rows of the main view: the notice when there is one, else the entries wrapped to {@code width}. */
    List<DetailRow> rows(int width) {
        return notice.map(SessionText::transcriptNotice).orElseGet(() -> SessionText.transcript(entries, width));
    }
}

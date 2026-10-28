package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.LastActivity;
import dev.maxenceng.lazyclaude.session.domain.LinkFacts;
import dev.maxenceng.lazyclaude.session.domain.TranscriptFacts;
import dev.maxenceng.lazyclaude.session.published.AiTitle;
import dev.maxenceng.lazyclaude.session.published.FirstPrompt;
import dev.maxenceng.lazyclaude.session.published.NoFirstPrompt;
import dev.maxenceng.lazyclaude.session.published.NoTitle;
import dev.maxenceng.lazyclaude.session.published.OpeningPrompt;
import dev.maxenceng.lazyclaude.session.published.ReadableTranscript;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.session.published.TranscriptReading;
import dev.maxenceng.lazyclaude.session.published.UnreadableTranscript;

/**
 * What is known of one transcript, as the cache file holds it: where it was last seen (path, size, modification time and
 * file key, which decide whether it is read again), how far it was read, and the facts drawn from it. Adding a field is a
 * bump of the cache's format version. Every nullable field is null while no line has carried it. {@code ticketRuns} are the raw
 * {@code /ticket} runs, kept whatever the board holds so that a ticket made later links its sessions without a transcript being
 * read again; they are empty, never null.
 */
record CachedTranscript(
        String path,
        long size,
        long modified,
        String fileKey,
        long offset,
        String sessionId,
        String aiTitle,
        String firstPrompt,
        Long lastActivity,
        List<CachedTicketRun> ticketRuns) {

    /** Whether the entry holds what every read of it relies on; a cache with one that does not is not trusted at all. */
    boolean isWhole() {
        if (path == null || fileKey == null || sessionId == null || size < 0 || offset < 0 || offset > size) {
            return false;
        }
        if (ticketRuns == null || !ticketRuns.stream().allMatch(run -> run != null && run.isWhole())) {
            return false;
        }
        try {
            Path file = Path.of(path);
            return file.isAbsolute() && file.getParent() != null && file.getParent().getParent() != null;
        } catch (InvalidPathException notAPath) {
            return false;
        }
    }

    boolean isAt(long otherSize, long otherModified, String otherFileKey) {
        return size == otherSize && modified == otherModified && fileKey.equals(otherFileKey);
    }

    /**
     * Empty when the transcript says too little to be a session: no session id or no entry with a timestamp. {@code unreadable} is
     * why the transcript cannot be read now; what earlier reads drew from it stays.
     */
    Optional<TranscriptFacts> toFacts(Optional<Reason> unreadable) {
        if (sessionId.isBlank() || lastActivity == null) {
            return Optional.empty();
        }
        return Optional.of(TranscriptFacts.builder()
                .id(new SessionId(sessionId))
                .title(title())
                .opening(opening())
                .reading(unreadable.<TranscriptReading>map(UnreadableTranscript::new).orElseGet(ReadableTranscript::new))
                .lastActivity(new LastActivity(Instant.ofEpochMilli(lastActivity)))
                .links(LinkFacts.builder()
                        .runs(ticketRuns.stream().map(CachedTicketRun::toDomain).toList())
                        .build())
                .build());
    }

    /** The title Claude Code gave the session, else what it was first asked, else nothing. */
    private SessionTitle title() {
        return said(aiTitle)
                .<SessionTitle>map(AiTitle::new)
                .or(() -> said(firstPrompt).map(FirstPrompt::new))
                .orElseGet(NoTitle::new);
    }

    private OpeningPrompt opening() {
        return said(firstPrompt).<OpeningPrompt>map(FirstPrompt::new).orElseGet(NoFirstPrompt::new);
    }

    /** A title or a prompt written blank says nothing, the same as none. */
    private static Optional<String> said(String text) {
        return Optional.ofNullable(text).filter(written -> !written.isBlank());
    }
}

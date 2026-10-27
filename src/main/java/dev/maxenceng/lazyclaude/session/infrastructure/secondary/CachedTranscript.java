package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.LastActivity;
import dev.maxenceng.lazyclaude.session.domain.LinkFacts;
import dev.maxenceng.lazyclaude.session.domain.LocatedInProject;
import dev.maxenceng.lazyclaude.session.domain.LocatedPastSession;
import dev.maxenceng.lazyclaude.session.domain.NamedTicketId;
import dev.maxenceng.lazyclaude.session.domain.NotLocated;
import dev.maxenceng.lazyclaude.session.domain.PastSession;
import dev.maxenceng.lazyclaude.session.domain.ProjectLocation;
import dev.maxenceng.lazyclaude.session.domain.StartedAt;
import dev.maxenceng.lazyclaude.session.published.ReadFacts;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionFacts;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.UnreadableFacts;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * What is known of one transcript, as the cache file holds it: where it was last seen (path, size, modification time and
 * file key, which decide whether it is read again), how far it was read, and the facts drawn from it. Adding a field is a
 * bump of the cache's format version. {@code projectRoot} is null when the {@code cwd} was located in no project, and
 * every other nullable field is null while no line has carried it. {@code branchIds} and {@code ticketRuns} are the raw link
 * facts, every ticket-id prefix of a recorded branch and every {@code /ticket} run, kept whatever the board holds so that a
 * ticket made later links its sessions without a transcript being read again; they are empty, never null.
 */
record CachedTranscript(
        String path,
        long size,
        long modified,
        String fileKey,
        long offset,
        String sessionId,
        String cwd,
        String projectRoot,
        String aiTitle,
        String firstPrompt,
        Long startedAt,
        Long lastActivity,
        List<String> branchIds,
        List<CachedTicketRun> ticketRuns) {

    /** Whether the entry holds what every read of it relies on; a cache with one that does not is not trusted at all. */
    boolean isWhole() {
        if (path == null || fileKey == null || sessionId == null || size < 0 || offset < 0 || offset > size) {
            return false;
        }
        if (branchIds == null || ticketRuns == null || !ticketRuns.stream().allMatch(run -> run != null && run.isWhole())) {
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
     * Empty when the transcript says too little to be a session: no session id, no {@code cwd}, no entry with a timestamp, or a
     * {@code cwd} that is no path. {@code unreadable} is why the transcript cannot be read now, which replaces its facts.
     */
    Optional<LocatedPastSession> toLocated(Optional<Reason> unreadable) {
        if (sessionId.isBlank() || cwd == null || startedAt == null || lastActivity == null) {
            return Optional.empty();
        }
        try {
            PastSession session = PastSession.builder()
                    .id(new SessionId(sessionId))
                    .facts(unreadable.<SessionFacts>map(UnreadableFacts::new).orElseGet(this::facts))
                    .directory(new WorkingDirectory(Path.of(cwd)))
                    .lastActivity(new LastActivity(Instant.ofEpochMilli(lastActivity)))
                    .startedAt(new StartedAt(Instant.ofEpochMilli(startedAt)))
                    .build();
            return Optional.of(LocatedPastSession.builder()
                    .session(session)
                    .location(location())
                    .build());
        } catch (InvalidPathException refused) {
            return Optional.empty();
        }
    }

    LinkFacts linkFacts() {
        return LinkFacts.builder()
                .runs(ticketRuns.stream().map(CachedTicketRun::toDomain).toList())
                .branchIds(branchIds.stream().map(NamedTicketId::new).toList())
                .build();
    }

    private SessionFacts facts() {
        return ReadFacts.of(said(aiTitle), said(firstPrompt));
    }

    /** A title or a prompt written blank says nothing, the same as none. */
    private static Optional<String> said(String text) {
        return Optional.ofNullable(text).filter(written -> !written.isBlank());
    }

    private ProjectLocation location() {
        return projectRoot == null ? new NotLocated() : new LocatedInProject(new Project(Path.of(projectRoot)));
    }
}

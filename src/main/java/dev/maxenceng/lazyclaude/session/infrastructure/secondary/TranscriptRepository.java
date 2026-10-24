package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.LinkFacts;
import dev.maxenceng.lazyclaude.session.domain.LocatedInProject;
import dev.maxenceng.lazyclaude.session.domain.LocatedPastSession;
import dev.maxenceng.lazyclaude.session.domain.NotLocated;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ProjectLocation;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.domain.TranscriptLocator;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.ProjectUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * The past sessions Claude Code's transcripts, {@code ~/.claude/projects/<folder>/<sessionId>.jsonl}, say. What was read is
 * kept in the cache: a transcript is read again only when its size, modification time or file key differ from the cache's,
 * and one that grew is read from the offset the cache holds. That relies on transcripts being append-only, which is
 * observed, not documented; a file that shrank or whose key changed is read in full. A {@code subagents/} transcript sits
 * a level deeper and is never met. It also answers the link facts of every transcript it knows, by session id. A transcript that cannot be read is dropped and read in full the next time it changes.
 */
public final class TranscriptRepository implements PastSessionPort {

    private static final String NO_FILE_KEY = "";
    private static final String CACHE_UNWRITABLE = "cannot write the cache %s: %s";
    private static final String ROOT_UNLISTABLE = "cannot list %s: %s";

    private final Path transcriptsRoot;
    private final TranscriptCacheFile cache;
    private final Map<String, CachedTranscript> known = new ConcurrentHashMap<>();
    private final Object loading = new Object();
    private volatile boolean loaded;

    public TranscriptRepository(Path transcriptsRoot, TranscriptCacheFile cache) {
        this.transcriptsRoot = transcriptsRoot;
        this.cache = cache;
    }

    @Override
    public List<LocatedPastSession> pastSessions() {
        ensureLoaded();
        return known.values().stream()
                .map(CachedTranscript::toLocated)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Map<SessionId, LinkFacts> linkFacts() {
        ensureLoaded();
        return known.values().stream()
                .filter(transcript -> !transcript.sessionId().isBlank())
                .collect(Collectors.toMap(transcript -> new SessionId(transcript.sessionId()), CachedTranscript::linkFacts, (first, second) -> first));
    }

    /** Reads again every transcript that is new or changed since the cache, and forgets those that are gone. */
    synchronized void rescanAll(TranscriptLocator locator, CacheFallback unwritable, RootFallback unlistable) {
        ensureLoaded();
        TranscriptListing listing = listTranscripts(unlistable);
        boolean forgot = known.keySet().removeIf(path -> listing.isGone(Path.of(path)));
        boolean refreshed = listing.present().stream().map(file -> refresh(file, locator)).reduce(false, Boolean::logicalOr);
        persist(forgot || refreshed, unwritable);
    }

    /** Reads again each of {@code files} that is new or changed, and forgets those that are gone. */
    synchronized void rescan(Collection<Path> files, TranscriptLocator locator, CacheFallback unwritable) {
        ensureLoaded();
        boolean changed = files.stream()
                .map(file -> Files.exists(file) ? refresh(file, locator) : known.remove(file.toString()) != null)
                .reduce(false, Boolean::logicalOr);
        persist(changed, unwritable);
    }

    /** Loads the cache once, under its own lock rather than the scan's, so a read never waits for a scan to finish. */
    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (loading) {
            if (!loaded) {
                known.putAll(cache.load().stream()
                        .collect(Collectors.toMap(CachedTranscript::path, Function.identity(), (first, second) -> second)));
                loaded = true;
            }
        }
    }

    private TranscriptListing listTranscripts(RootFallback unlistable) {
        if (!Files.isDirectory(transcriptsRoot)) {
            return TranscriptListing.none();
        }
        try (Stream<Path> folders = Files.list(transcriptsRoot)) {
            return folders.filter(Files::isDirectory)
                    .map(TranscriptRepository::listFolder)
                    .reduce(TranscriptListing.none(), TranscriptListing::and);
        } catch (IOException unreadable) {
            return unlisted(unreadable, unlistable);
        } catch (UncheckedIOException unreadable) {
            return unlisted(unreadable.getCause(), unlistable);
        }
    }

    private TranscriptListing unlisted(IOException cause, RootFallback unlistable) {
        unlistable.unlistable(new Reason(ROOT_UNLISTABLE.formatted(transcriptsRoot, FileFailure.causeOf(cause))));
        return TranscriptListing.unlistable(transcriptsRoot);
    }

    private static TranscriptListing listFolder(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return TranscriptListing.found(files.filter(TranscriptFile::isTranscript)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toUnmodifiableSet()));
        } catch (IOException | UncheckedIOException unreadable) {
            return TranscriptListing.unlistable(folder);
        }
    }

    private boolean refresh(Path file, TranscriptLocator locator) {
        String key = file.toString();
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            String fileKey = String.valueOf(attributes.fileKey());
            CachedTranscript before = known.get(key);
            long modified = attributes.lastModifiedTime().toMillis();
            if (before != null && before.isAt(attributes.size(), modified, fileKey)) {
                return false;
            }
            CachedTranscript after = read(file, before, attributes.size(), modified, fileKey, locator);
            known.put(key, after);
            return true;
        } catch (IOException vanishedOrUnreadable) {
            return known.remove(key) != null;
        } catch (ProjectUnavailableException cannotLocate) {
            // deferred: a cwd git fails on is read again and asked again on every scan, with no warning — close it when that shows up as load, since it never shows a wrong row.
            return false;
        }
    }

    private static CachedTranscript read(
            Path file, CachedTranscript before, long size, long modified, String fileKey, TranscriptLocator locator)
            throws IOException {
        boolean grew = before != null && before.fileKey().equals(fileKey) && size >= before.size();
        CachedTranscript base = grew ? before : empty(file);
        TranscriptTail tail = new TranscriptTail(base);
        long offset = TranscriptLines.read(file, grew ? before.offset() : 0, tail::accept);
        CachedTranscript read = tail.into(base, size, modified, fileKey, offset);
        return located(read, before, locator);
    }

    private static CachedTranscript empty(Path file) {
        String sessionId = TranscriptFile.sessionIdOf(file);
        return new CachedTranscript(file.toString(), 0, 0, NO_FILE_KEY, 0, sessionId, null, null, null, null, null, null, List.of(), List.of());
    }

    private static CachedTranscript located(CachedTranscript read, CachedTranscript before, TranscriptLocator locator) {
        if (read.cwd() == null) {
            return read;
        }
        if (before != null && read.cwd().equals(before.cwd())) {
            return withProjectRoot(read, before.projectRoot());
        }
        try {
            ProjectLocation location = locator.locate(new WorkingDirectory(Path.of(read.cwd())));
            String root = switch (location) {
                case LocatedInProject inProject -> inProject.project().root().toString();
                case NotLocated nowhere -> null;
            };
            return withProjectRoot(read, root);
        } catch (InvalidPathException notAPath) {
            return withProjectRoot(read, null);
        }
    }

    private static CachedTranscript withProjectRoot(CachedTranscript transcript, String projectRoot) {
        return new CachedTranscript(
                transcript.path(),
                transcript.size(),
                transcript.modified(),
                transcript.fileKey(),
                transcript.offset(),
                transcript.sessionId(),
                transcript.cwd(),
                projectRoot,
                transcript.aiTitle(),
                transcript.firstPrompt(),
                transcript.startedAt(),
                transcript.lastActivity(),
                transcript.branchIds(),
                transcript.ticketRuns());
    }

    private void persist(boolean changed, CacheFallback unwritable) {
        if (!changed) {
            return;
        }
        try {
            cache.write(known.values());
        } catch (IOException e) {
            unwritable.unwritable(new Reason(CACHE_UNWRITABLE.formatted(cache.file(), FileFailure.causeOf(e))));
        }
    }
}

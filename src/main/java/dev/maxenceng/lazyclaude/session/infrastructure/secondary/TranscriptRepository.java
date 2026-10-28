package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.LoggedTranscripts;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * What the transcripts of the logged sessions, {@code ~/.claude/projects/<folder>/<sessionId>.jsonl}, say. A directory listing
 * finds them by name; only a logged id's transcript is read, and what was read is kept in the cache: a transcript is read again
 * only when its size, modification time or file key differ from the cache's, and one that grew is read from the offset the cache
 * holds. That relies on transcripts being append-only, which is observed, not documented; a file that shrank or whose key
 * changed is read in full. A {@code subagents/} transcript sits a level deeper and is never met. A transcript read before that
 * cannot be read now keeps its row, with why in place of its facts, until it reads again or is gone; one never read is not
 * listed until it reads, and one that vanished is dropped. Why is held in memory, never in the cache. A session is called gone
 * only once a scan has listed every transcript folder and found none of its name: a folder it could not list may hold it.
 */
public final class TranscriptRepository implements PastSessionPort {

    private static final String NO_FILE_KEY = "";
    private static final String CACHE_UNWRITABLE = "cannot write the cache %s: %s";
    private static final String ROOT_UNLISTABLE = "cannot list %s: %s";

    private final Path transcriptsRoot;
    private final TranscriptCacheFile cache;
    private final Map<String, CachedTranscript> known = new ConcurrentHashMap<>();
    private final Map<String, Reason> unreadable = new ConcurrentHashMap<>();
    private final Set<Path> present = ConcurrentHashMap.newKeySet();
    private final Object loading = new Object();
    private volatile boolean loaded;
    private volatile boolean listedWhole;

    public TranscriptRepository(Path transcriptsRoot, TranscriptCacheFile cache) {
        this.transcriptsRoot = transcriptsRoot;
        this.cache = cache;
    }

    @Override
    public LoggedTranscripts transcriptsOf(Set<SessionId> logged) {
        ensureLoaded();
        Set<String> names = present.stream().map(TranscriptFile::sessionIdOf).collect(Collectors.toUnmodifiableSet());
        return LoggedTranscripts.builder()
                .read(known.values().stream()
                        .filter(transcript -> logged.contains(new SessionId(transcript.sessionId())))
                        .flatMap(transcript -> transcript
                                .toFacts(Optional.ofNullable(unreadable.get(transcript.path())))
                                .stream())
                        .toList())
                .gone(logged.stream()
                        .filter(id -> listedWhole && !names.contains(id.value()))
                        .toList())
                .build();
    }

    /**
     * Reads again every transcript of a {@code logged} session that is new or changed since the cache, and forgets those that are
     * gone: one another project logged stays, so two lazyclaude on two projects keep each other's offsets in the shared cache.
     */
    synchronized void rescanAll(Set<SessionId> logged, CacheFallback unwritable, RootFallback unlistable) {
        ensureLoaded();
        TranscriptListing listing = listTranscripts(unlistable);
        present.removeIf(listing::isGone);
        present.addAll(listing.present());
        listedWhole = listing.unlisted().isEmpty();
        boolean forgot = forgetting(listing::isGone);
        boolean refreshed = listing.present().stream()
                .filter(file -> isLogged(file, logged))
                .map(this::refresh)
                .reduce(false, Boolean::logicalOr);
        persist(forgot || refreshed, unwritable);
    }

    /** Reads again each of {@code files} of a {@code logged} session that is new or changed, and forgets those that are gone. */
    synchronized void rescan(Collection<Path> files, Set<SessionId> logged, CacheFallback unwritable) {
        ensureLoaded();
        boolean changed = files.stream()
                .map(file -> Files.notExists(file) ? gone(file) : present(file, logged))
                .reduce(false, Boolean::logicalOr);
        persist(changed, unwritable);
    }

    private boolean present(Path file, Set<SessionId> logged) {
        present.add(file);
        return isLogged(file, logged) && refresh(file);
    }

    private boolean gone(Path file) {
        present.remove(file);
        return forget(file.toString());
    }

    private static boolean isLogged(Path file, Set<SessionId> logged) {
        return logged.contains(new SessionId(TranscriptFile.sessionIdOf(file)));
    }

    private boolean forgetting(Predicate<Path> forgotten) {
        unreadable.keySet().removeIf(path -> forgotten.test(Path.of(path)));
        return known.keySet().removeIf(path -> forgotten.test(Path.of(path)));
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

    private boolean refresh(Path file) {
        String key = file.toString();
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            String fileKey = String.valueOf(attributes.fileKey());
            CachedTranscript before = known.get(key);
            long modified = attributes.lastModifiedTime().toMillis();
            if (before != null && before.isAt(attributes.size(), modified, fileKey)) {
                return false;
            }
            known.put(key, read(file, before, attributes.size(), modified, fileKey));
            unreadable.remove(key);
            return true;
        } catch (NoSuchFileException vanished) {
            return forget(key);
        } catch (IOException cannotRead) {
            if (known.containsKey(key)) {
                unreadable.put(key, new Reason(FileFailure.causeOf(cannotRead)));
            }
            return false;
        }
    }

    private boolean forget(String key) {
        unreadable.remove(key);
        return known.remove(key) != null;
    }

    private static CachedTranscript read(Path file, CachedTranscript before, long size, long modified, String fileKey)
            throws IOException {
        boolean grew = before != null && before.fileKey().equals(fileKey) && size >= before.size();
        CachedTranscript base = grew ? before : empty(file);
        TranscriptTail tail = new TranscriptTail(base);
        long offset = TranscriptLines.read(file, grew ? before.offset() : 0, tail::accept);
        return tail.into(base, size, modified, fileKey, offset);
    }

    private static CachedTranscript empty(Path file) {
        return new CachedTranscript(file.toString(), 0, 0, NO_FILE_KEY, 0, TranscriptFile.sessionIdOf(file), null, null, null, List.of());
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

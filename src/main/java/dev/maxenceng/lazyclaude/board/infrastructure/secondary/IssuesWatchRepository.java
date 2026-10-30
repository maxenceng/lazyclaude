package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Polls the most recently updated issue of the project's repository on one daemon thread, with the {@code ETag} of the last answer in
 * an {@code If-None-Match}: GitHub answers 304 and charges nothing while no issue changed. The first check only takes the {@code ETag}
 * the board was read at; a check that fails forgets it, so the next good one counts as a change and the board is read again.
 */
public final class IssuesWatchRepository implements BoardWatchPort, AutoCloseable {

    static final Duration INTERVAL = Duration.ofSeconds(30);

    private static final String WORKER_NAME = "board-watch";
    private static final String NEWEST_UPDATED =
            "repos/{owner}/{repo}/issues?state=all&per_page=1&sort=updated&direction=desc";
    private static final List<String> HEADERS_ONLY = List.of("api", "--include", "--silent", NEWEST_UPDATED);
    private static final String IF_NONE_MATCH = "If-None-Match: %s";
    private static final String HEADER_FLAG = "--header";
    private static final Pattern STATUS_LINE = Pattern.compile("^HTTP/\\S+ 304", Pattern.MULTILINE);
    private static final Pattern ETAG = Pattern.compile("^etag: (.+?)\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private final GhCommand gh;
    private final Duration interval;
    private final List<Thread> workers = new CopyOnWriteArrayList<>();
    private Optional<String> known = Optional.empty();
    private boolean taken;

    public IssuesWatchRepository() {
        this(new GhCommand(), INTERVAL);
    }

    IssuesWatchRepository(GhCommand gh, Duration interval) {
        this.gh = gh;
        this.interval = interval;
    }

    @Override
    public void watch(Project project, Runnable changed) {
        workers.add(Thread.ofPlatform().daemon().name(WORKER_NAME).start(() -> loop(project, changed)));
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
    }

    private void loop(Project project, Runnable changed) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (changedSinceLastCheck(project)) {
                    changed.run();
                }
                Thread.sleep(interval);
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    /** One check: whether the issues changed since the last one, or this one could not tell. */
    synchronized boolean changedSinceLastCheck(Project project) {
        Optional<String> before = known;
        boolean first = !taken;
        taken = true;
        try {
            GhResult answered = gh.run(project, arguments(before));
            if (STATUS_LINE.matcher(answered.outputText()).find()) {
                return false;
            }
            known = answered.succeeded() ? etagOf(answered) : Optional.empty();
            return known.isEmpty() || !first && !known.equals(before);
        } catch (BacklogUnavailableException cannotCheck) {
            known = Optional.empty();
            return true;
        }
    }

    private static List<String> arguments(Optional<String> etag) {
        return Stream.concat(HEADERS_ONLY.stream(), etag.stream().flatMap(tag -> Stream.of(HEADER_FLAG, IF_NONE_MATCH.formatted(tag))))
                .toList();
    }

    private static Optional<String> etagOf(GhResult answered) {
        Matcher header = ETAG.matcher(answered.outputText());
        return header.find() ? Optional.of(header.group(1)) : Optional.empty();
    }
}

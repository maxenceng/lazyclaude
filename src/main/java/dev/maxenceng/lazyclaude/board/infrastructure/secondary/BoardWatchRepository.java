package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.domain.WatchFallback;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatcher;

/**
 * Watches what the board is read from on a {@link TreeWatcher}: the root checkout's {@code docs/backlog/}, git's common directory
 * for the refs and the worktrees, and the {@code docs/backlog/} of each worktree git lists there, where only the ticket's own file
 * counts. The worktrees are read from {@code worktrees/<name>/} again after every batch, so one added later is watched and one
 * removed is let go; a worktree whose {@code docs/backlog/} is not there yet is looked for again at the next batch.
 */
public final class BoardWatchRepository implements BoardWatchPort, AutoCloseable {

    private static final Path BACKLOG = Path.of("docs", "backlog");
    private static final String GIT_DIRECTORY = ".git";
    private static final String WORKTREES = "worktrees";
    private static final String LINKED_GIT_FILE = "gitdir";
    private static final String HEAD = "HEAD";
    private static final String BRANCH_REF = "ref: refs/heads/";

    private final TreeWatcher watcher;
    private final Map<Path, TicketBranch> watchedWorktrees = new ConcurrentHashMap<>();

    public BoardWatchRepository() {
        this(new TreeWatcher());
    }

    BoardWatchRepository(TreeWatcher watcher) {
        this.watcher = watcher;
    }

    @Override
    public void watch(Project project, Runnable changed, WatchFallback fallback) {
        Path gitDirectory = project.root().resolve(GIT_DIRECTORY);
        watcher.start(
                () -> {
                    followWorktrees(gitDirectory);
                    changed.run();
                },
                (_, cause, every) -> fallback.polling(new Reason(cause), every));
        watcher.add(BacklogScope.ofRootCheckout(project.root().resolve(BACKLOG)));
        watcher.add(new GitDirectoryScope(gitDirectory));
        followWorktrees(gitDirectory);
    }

    @Override
    public void close() {
        watcher.close();
    }

    /** Watches the backlog of each worktree git lists now, for the branch it holds now, and lets go of the ones it no longer lists. */
    private synchronized void followWorktrees(Path gitDirectory) {
        Map<Path, TicketBranch> listed = worktreeBacklogs(gitDirectory.resolve(WORKTREES));
        watchedWorktrees.entrySet().stream()
                .filter(watched -> !watched.getValue().equals(listed.get(watched.getKey())))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(this::letGo);
        listed.entrySet().stream()
                .filter(worktree -> !watcher.covers(worktree.getKey()))
                .forEach(worktree -> watch(worktree.getKey(), worktree.getValue()));
    }

    private void watch(Path backlog, TicketBranch ticketBranch) {
        watcher.add(BacklogScope.ofTicketWorktree(backlog, ticketBranch));
        if (watcher.covers(backlog)) {
            watchedWorktrees.put(backlog, ticketBranch);
        }
    }

    private void letGo(Path backlog) {
        watcher.drop(backlog);
        watchedWorktrees.remove(backlog);
    }

    private static Map<Path, TicketBranch> worktreeBacklogs(Path worktrees) {
        try (Stream<Path> administrative = Files.list(worktrees)) {
            return administrative
                    .flatMap(directory -> backlogOf(directory).stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, _) -> first));
        } catch (IOException noWorktrees) {
            return Map.of();
        }
    }

    /** The backlog of the worktree an administrative directory describes, with the branch it holds; nothing for one that holds no branch. */
    private static Optional<Map.Entry<Path, TicketBranch>> backlogOf(Path administrative) {
        return recorded(administrative.resolve(LINKED_GIT_FILE))
                .map(Path::of)
                .map(Path::getParent)
                .flatMap(worktree -> recorded(administrative.resolve(HEAD))
                        .filter(head -> head.startsWith(BRANCH_REF) && head.length() > BRANCH_REF.length())
                        .map(head -> Map.entry(worktree.resolve(BACKLOG), new TicketBranch(head.substring(BRANCH_REF.length())))));
    }

    private static Optional<String> recorded(Path file) {
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8).strip());
        } catch (IOException unreadable) {
            return Optional.empty();
        }
    }
}

package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.file.Path;
import java.util.function.Predicate;

import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeScope;

/** One {@code docs/backlog/} directory, flat, and the files in it a change to which is worth reading the board again for. */
final class BacklogScope implements TreeScope {

    private static final String MARKDOWN = ".md";
    private static final String HIDDEN_PREFIX = "_";

    private final Path backlog;
    private final Predicate<String> files;

    private BacklogScope(Path backlog, Predicate<String> files) {
        this.backlog = backlog;
        this.files = files;
    }

    /** The root checkout's backlog, where any ticket file counts. */
    static BacklogScope ofRootCheckout(Path backlog) {
        return new BacklogScope(backlog, name -> name.endsWith(MARKDOWN) && !name.startsWith(HIDDEN_PREFIX));
    }

    /** A ticket worktree's backlog, where only the file of the ticket the worktree is for counts. */
    static BacklogScope ofTicketWorktree(Path backlog, TicketBranch ticketBranch) {
        return new BacklogScope(
                backlog, name -> name.endsWith(MARKDOWN) && TicketBranch.of(new FileName(name)).equals(ticketBranch));
    }

    @Override
    public Path root() {
        return backlog;
    }

    @Override
    public boolean descends(Path directory) {
        return directory.equals(backlog);
    }

    @Override
    public boolean counts(Path entry) {
        return backlog.equals(entry.getParent()) && files.test(entry.getFileName().toString());
    }
}

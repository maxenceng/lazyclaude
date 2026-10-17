package dev.maxenceng.lazyclaude.board.application;

import java.util.Optional;
import java.util.Set;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.BoardManager;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyPort;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Reads the board of the project a working directory belongs to, each ticket from the copy that stands for it. */
public final class BoardApplicationService {

    private final BoardManager manager;

    public BoardApplicationService(
            ProjectLocatorPort locator,
            BacklogPort backlog,
            BranchCopyPort copies,
            BranchChangesPort changes,
            BoardWatchPort watch,
            WarningPort warnings) {
        this.manager = new BoardManager(locator, backlog, copies, changes, watch, warnings);
    }

    public Board boardFor(WorkingDirectory from, Set<TicketBranch> merged) {
        return manager.boardFor(from, merged);
    }

    /** The board read again, or nothing when it cannot be, so a board already on screen stays; the manager decides what that costs a warning. */
    public Optional<Board> boardReadAgainFor(WorkingDirectory from, Set<TicketBranch> merged) {
        try {
            return Optional.of(manager.boardFor(from, merged));
        } catch (DomainException cannotRead) {
            manager.boardUnreadable(new Reason(cannotRead.getMessage()));
            return Optional.empty();
        }
    }

    /** Runs {@code changed} after each batch of changes to what the board is read from. */
    public void subscribe(WorkingDirectory from, Runnable changed) {
        manager.watch(from, changed);
    }
}

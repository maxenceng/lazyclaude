package dev.maxenceng.lazyclaude.board.application;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.BoardLookup;
import dev.maxenceng.lazyclaude.board.domain.BoardManager;
import dev.maxenceng.lazyclaude.board.domain.BoardReading;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.domain.NoProject;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.ProjectUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Reads the board of the project a working directory belongs to from its GitHub issues. */
public final class BoardApplicationService {

    private final BoardManager manager;

    public BoardApplicationService(
            ProjectLocatorPort locator,
            BacklogPort backlog,
            BranchChangesPort changes,
            BoardWatchPort watch,
            WarningPort warnings) {
        this.manager = new BoardManager(locator, backlog, changes, watch, warnings);
    }

    /**
     * The board, or the project without tickets and why the issues cannot be read, or why there is no project; nothing is kept from an
     * earlier read.
     */
    public BoardLookup boardFor(WorkingDirectory from) {
        try {
            return manager.boardFor(from);
        } catch (BacklogUnavailableException cannotRead) {
            return manager.unavailableBoard(from, new Reason(cannotRead.getMessage()));
        } catch (ProjectNotFoundException | ProjectUnavailableException noProject) {
            return new NoProject(new Reason(noProject.getMessage()));
        }
    }

    /**
     * The board read again after a change, or nothing when what is shown stays. A board with tickets is kept when the read fails, the
     * warning saying why; one without is read whole, so its reason stays current, and kept with that warning only when the project
     * itself is gone.
     */
    public Optional<BoardReading> boardAfterChange(WorkingDirectory from, boolean boardShown) {
        if (boardShown) {
            return boardReadAgainFor(from);
        }
        return switch (boardFor(from)) {
            case BoardReading read -> Optional.of(read);
            case NoProject none -> kept(none.reason());
        };
    }

    private Optional<BoardReading> boardReadAgainFor(WorkingDirectory from) {
        try {
            return Optional.of(manager.boardFor(from));
        } catch (DomainException cannotRead) {
            return kept(new Reason(cannotRead.getMessage()));
        }
    }

    /** What is shown stays; the manager decides what that costs a warning. */
    private Optional<BoardReading> kept(Reason cause) {
        manager.boardUnreadable(cause);
        return Optional.empty();
    }

    /** Runs {@code changed} whenever the project's issues changed. */
    public void subscribe(WorkingDirectory from, Runnable changed) {
        manager.watch(from, changed);
    }
}

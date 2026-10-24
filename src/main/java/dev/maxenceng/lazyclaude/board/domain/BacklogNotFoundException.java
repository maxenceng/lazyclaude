package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The project has no backlog directory, so there is no board to show. */
public final class BacklogNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public BacklogNotFoundException(Path backlog) {
        super(DomainErrorStatus.NOT_FOUND, "the main worktree has no %s".formatted(backlog));
    }
}

package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The project has no GitHub remote, so there is no backlog to show. */
public final class BacklogNotFoundException extends DomainException {

    public BacklogNotFoundException() {
        super(DomainErrorStatus.NOT_FOUND, "this project has no GitHub remote, so it has no backlog");
    }
}

package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The backlog directory exists but its files cannot be listed. */
public final class BacklogUnavailableException extends DomainException {

    private static final long serialVersionUID = 1L;

    public BacklogUnavailableException(Path backlog, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, "cannot list %s: %s".formatted(backlog, cause.getMessage()), cause);
    }
}

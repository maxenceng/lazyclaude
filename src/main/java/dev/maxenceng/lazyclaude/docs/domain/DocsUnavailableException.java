package dev.maxenceng.lazyclaude.docs.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** The docs directory exists but its files cannot be listed. */
public final class DocsUnavailableException extends DomainException {

    private static final long serialVersionUID = 1L;

    public DocsUnavailableException(Path docs, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, "cannot list %s: %s".formatted(docs, cause.getMessage()), cause);
    }
}

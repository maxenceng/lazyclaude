package dev.maxenceng.lazyclaude.error.domain;

import java.nio.file.Path;

/** A path a domain type holds as absolute was relative, so it would mean something else from another directory. */
public final class RelativePathException extends AssertionException {

    private static final long serialVersionUID = 1L;

    RelativePathException(String field, Path value) {
        super(field, "%s is absolute, got %s".formatted(field, value));
    }
}

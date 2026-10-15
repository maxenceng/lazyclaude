package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

/** {@code git} could not be run, or its run was cut short; the caller says what that leaves undone. */
public final class GitNotRunException extends RuntimeException {

    public GitNotRunException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

package dev.maxenceng.lazyclaude.board.domain;

/** There is no project to show a board for; the message says why. */
public final class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String reason) {
        super(reason);
    }

    public ProjectNotFoundException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

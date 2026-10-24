package dev.maxenceng.lazyclaude.error.domain;

/**
 * Base type for violations of a business rule. Its message is written for the person who
 * started lazyclaude, since it may be all they see.
 */
public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final DomainErrorStatus status;

    protected DomainException(DomainErrorStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected DomainException(DomainErrorStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public DomainErrorStatus status() {
        return status;
    }
}

package dev.maxenceng.lazyclaude.error.domain;

/** An input refused by {@link Assert}; {@link #field()} names what was refused. */
public abstract class AssertionException extends DomainException {

    private final String field;

    protected AssertionException(String field, String message) {
        super(DomainErrorStatus.INVALID, message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

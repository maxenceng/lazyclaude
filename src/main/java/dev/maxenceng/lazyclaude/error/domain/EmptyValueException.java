package dev.maxenceng.lazyclaude.error.domain;

/** A collection a domain type cannot exist without held nothing. */
public final class EmptyValueException extends AssertionException {

    private static final long serialVersionUID = 1L;

    EmptyValueException(String field) {
        super(field, "%s is empty".formatted(field));
    }
}

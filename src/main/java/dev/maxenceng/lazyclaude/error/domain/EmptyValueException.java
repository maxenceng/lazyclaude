package dev.maxenceng.lazyclaude.error.domain;

/** A collection a domain type cannot exist without held nothing. */
public final class EmptyValueException extends AssertionException {

    EmptyValueException(String field) {
        super(field, "%s is empty".formatted(field));
    }
}

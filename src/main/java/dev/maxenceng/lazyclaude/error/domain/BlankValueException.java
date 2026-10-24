package dev.maxenceng.lazyclaude.error.domain;

/** A text value a domain type cannot exist without was empty or only whitespace. */
public final class BlankValueException extends AssertionException {

    private static final long serialVersionUID = 1L;

    BlankValueException(String field) {
        super(field, "%s is blank".formatted(field));
    }
}

package dev.maxenceng.lazyclaude.error.domain;

/** A value a domain type cannot exist without was not given. */
public final class MissingMandatoryValueException extends AssertionException {

    MissingMandatoryValueException(String field) {
        super(field, "%s is mandatory and was not set".formatted(field));
    }
}

package dev.maxenceng.lazyclaude.error.domain;

/** A count a domain type holds was below zero. */
public final class NegativeValueException extends AssertionException {

    NegativeValueException(String field, int value) {
        super(field, "%s is never negative, got %d".formatted(field, value));
    }
}

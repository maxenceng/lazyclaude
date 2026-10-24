package dev.maxenceng.lazyclaude.error.domain;

/** A number a domain type holds no greater than another was greater, so the two no longer read in order. */
public final class AfterBoundException extends AssertionException {

    private static final long serialVersionUID = 1L;

    AfterBoundException(String field, int value, int bound) {
        super(field, "%s is at most %d, got %d".formatted(field, bound, value));
    }
}

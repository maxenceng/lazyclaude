package dev.maxenceng.lazyclaude.error.domain;

/** A text value does not have the shape its field requires. */
public final class NotMatchingExpectedPatternException extends AssertionException {

    NotMatchingExpectedPatternException(String field, String value, String shape) {
        super(field, "%s '%s' is not %s".formatted(field, value, shape));
    }
}

package dev.maxenceng.lazyclaude.error.domain;

import java.util.regex.Pattern;

/**
 * The input assertions a domain type makes on construction. Business rules get their own
 * exception instead; this covers only the basic shape of a value.
 */
public final class Assert {

    private Assert() {}

    public static void notNull(String field, Object input) {
        if (input == null) {
            throw new MissingMandatoryValueException(field);
        }
    }

    public static void notBlank(String field, String input) {
        notNull(field, input);
        if (input.isBlank()) {
            throw new BlankValueException(field);
        }
    }

    /** {@code shape} says in words what {@code pattern} accepts, for a message a person reads. */
    public static void matchesPattern(String field, String input, Pattern pattern, String shape) {
        notNull(field, input);
        if (!pattern.matcher(input).matches()) {
            throw new NotMatchingExpectedPatternException(field, input, shape);
        }
    }
}

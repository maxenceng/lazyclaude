package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.util.List;
import java.util.stream.Stream;

/**
 * The fields a record read from outside lacks, by name, gathered before anything is built from it: a gap in what a tool wrote is the
 * adapter's to report, never the domain's to refuse (ADR 0043).
 */
public record MissingFields(List<String> names) {

    private static final String SEPARATOR = ", ";

    /** None missing yet. */
    public MissingFields() {
        this(List.of());
    }

    /** These, with {@code name} when {@code value} is absent. */
    public MissingFields absent(String name, Object value) {
        return value == null ? with(name) : this;
    }

    /** These, with {@code name} when {@code value} is absent or blank. */
    public MissingFields blank(String name, String value) {
        return value == null || value.isBlank() ? with(name) : this;
    }

    /** These and {@code others}. */
    public MissingFields and(MissingFields others) {
        return new MissingFields(Stream.concat(names.stream(), others.names().stream()).toList());
    }

    public boolean any() {
        return !names.isEmpty();
    }

    /** The names, as a person reads them. */
    public String listed() {
        return String.join(SEPARATOR, names);
    }

    private MissingFields with(String name) {
        return new MissingFields(Stream.concat(names.stream(), Stream.of(name)).toList());
    }
}

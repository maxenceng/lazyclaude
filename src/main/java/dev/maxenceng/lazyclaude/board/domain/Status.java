package dev.maxenceng.lazyclaude.board.domain;

import java.util.Arrays;

/** A ticket's place in its lifecycle; declaration order is lifecycle order. */
public enum Status {
    DRAFT("draft"),
    TODO("todo"),
    IN_PROGRESS("in-progress"),
    IN_REVIEW("in-review"),
    DONE("done");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Status parse(String label) {
        return Arrays.stream(values())
                .filter(status -> status.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("status '" + label + "' is not one of "
                        + String.join(", ", Arrays.stream(values()).map(Status::label).toList())));
    }
}

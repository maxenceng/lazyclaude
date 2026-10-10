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

    /** The end of the lifecycle: nothing moves a ticket on from here. */
    public boolean finished() {
        return this == DONE;
    }

    public static Status parse(String label) {
        return Arrays.stream(values())
                .filter(status -> status.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new UnknownStatusException(label));
    }
}

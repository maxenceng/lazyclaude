package dev.maxenceng.lazyclaude.board.published;

import java.util.Arrays;
import java.util.List;

/** A ticket's place in its lifecycle; declaration order is lifecycle order. */
public enum Status {
    DRAFT("draft", Verb.REFINE),
    TODO("todo", Verb.START),
    IN_PROGRESS("in-progress", Verb.REVIEW),
    IN_REVIEW("in-review", Verb.RESPOND, Verb.DONE),
    DONE("done");

    private final String label;
    private final List<Verb> verbs;

    Status(String label, Verb... verbs) {
        this.label = label;
        this.verbs = List.of(verbs);
    }

    public String label() {
        return label;
    }

    /** The verbs {@code /ticket} accepts for a ticket in this status, in the order they are offered; none once it is done. */
    public List<Verb> verbs() {
        return verbs;
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

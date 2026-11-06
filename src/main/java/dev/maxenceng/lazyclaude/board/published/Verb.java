package dev.maxenceng.lazyclaude.board.published;

import java.util.Arrays;
import java.util.Optional;

/** What {@code /ticket} can be asked to do to a ticket. */
public enum Verb {
    REFINE("refine"),
    START("start"),
    REVIEW("review"),
    DESIGN("design"),
    RESPOND("respond"),
    DONE("done");

    private final String label;

    Verb(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** The verb {@code label} names, if any does. */
    public static Optional<Verb> find(String label) {
        return Arrays.stream(values()).filter(verb -> verb.label.equals(label)).findFirst();
    }

    /**
     * Whether {@code /ticket} accepts this verb for a ticket in {@code status}: the gates {@code ticket-tracker ready} enforces for
     * {@code refine}, {@code start} and {@code design}, and no verb at all once the ticket is done.
     */
    public boolean mayBeOfferedIn(Status status) {
        return !status.finished()
                && switch (this) {
                    case REFINE -> status == Status.DRAFT;
                    case START -> status == Status.TODO;
                    case DESIGN -> status == Status.IN_PROGRESS;
                    case REVIEW, RESPOND, DONE -> true;
                };
    }
}

package dev.maxenceng.lazyclaude.board.published;

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
}

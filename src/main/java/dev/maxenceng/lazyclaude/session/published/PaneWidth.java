package dev.maxenceng.lazyclaude.session.published;

/** How many cells wide a pane is; a pane has at least one, since a pty cannot be smaller. */
public record PaneWidth(int cells) {

    private static final int MINIMUM = 1;

    public PaneWidth {
        cells = Math.max(MINIMUM, cells);
    }
}

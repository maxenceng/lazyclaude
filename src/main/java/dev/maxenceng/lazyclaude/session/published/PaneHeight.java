package dev.maxenceng.lazyclaude.session.published;

/** How many cells tall a pane is; a pane has at least one, since a pty cannot be smaller. */
public record PaneHeight(int cells) {

    private static final int MINIMUM = 1;

    public PaneHeight {
        cells = Math.max(MINIMUM, cells);
    }
}

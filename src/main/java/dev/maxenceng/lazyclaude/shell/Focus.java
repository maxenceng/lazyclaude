package dev.maxenceng.lazyclaude.shell;

/** Where keyboard focus is: one of the four left panels, or the two a review swaps them for, or the main view or dock on the right. */
enum Focus {
    BOARD(Column.LEFT, false),
    SESSIONS(Column.LEFT, false),
    DOCS(Column.LEFT, false),
    ELSEWHERE(Column.LEFT, false),
    FILES(Column.LEFT, true),
    COMMENTS(Column.LEFT, true),
    MAIN(Column.RIGHT, false),
    DOCK(Column.RIGHT, false);

    private static final int DOWN = 1;
    private static final int UP = -1;

    private final Column column;
    private final boolean reviewing;

    Focus(Column column, boolean reviewing) {
        this.column = column;
        this.reviewing = reviewing;
    }

    Column column() {
        return column;
    }

    /** Whether it is one of the panels a review shows in place of the four, which do not stand beside them. */
    boolean reviewing() {
        return reviewing;
    }

    Focus below() {
        return neighbour(DOWN);
    }

    Focus above() {
        return neighbour(UP);
    }

    private Focus neighbour(int step) {
        int index = ordinal() + step;
        boolean inColumn = index >= 0
                && index < values().length
                && values()[index].column == column
                && values()[index].reviewing == reviewing;
        return inColumn ? values()[index] : this;
    }
}

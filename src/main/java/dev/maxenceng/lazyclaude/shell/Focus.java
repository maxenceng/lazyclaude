package dev.maxenceng.lazyclaude.shell;

/** Where keyboard focus is: one of the four left panels, or the main view or dock on the right. */
enum Focus {
    BOARD(Column.LEFT),
    SESSIONS(Column.LEFT),
    DOCS(Column.LEFT),
    ELSEWHERE(Column.LEFT),
    MAIN(Column.RIGHT),
    DOCK(Column.RIGHT);

    enum Column {
        LEFT,
        RIGHT
    }

    private final Column column;

    Focus(Column column) {
        this.column = column;
    }

    Column column() {
        return column;
    }

    Focus below() {
        return neighbour(1);
    }

    Focus above() {
        return neighbour(-1);
    }

    private Focus neighbour(int step) {
        int index = ordinal() + step;
        boolean inColumn = index >= 0 && index < values().length && values()[index].column == column;
        return inColumn ? values()[index] : this;
    }
}

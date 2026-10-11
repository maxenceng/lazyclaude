package dev.maxenceng.lazyclaude.shell;

/** The past sessions beyond the newest few, folded into one row that {@code enter} unfolds. */
record OlderSessionsRow(int count) implements SessionRow {

    private static final String NAME = "older";

    @Override
    public RowKey key() {
        return new RowKey(RowKind.OLDER, NAME);
    }
}

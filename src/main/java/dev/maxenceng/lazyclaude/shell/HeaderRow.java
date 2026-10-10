package dev.maxenceng.lazyclaude.shell;

/** A group's header: its name and count, with a marker saying whether it is folded. */
record HeaderRow(String group, int count, boolean folded) implements Row {

    private static final String FOLDED_MARKER = "▸";
    private static final String UNFOLDED_MARKER = "▾";

    @Override
    public RowKey key() {
        return new RowKey(RowKind.GROUP, group);
    }

    @Override
    public String text() {
        return "%s %s (%d)".formatted(folded ? FOLDED_MARKER : UNFOLDED_MARKER, group, count);
    }
}

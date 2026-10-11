package dev.maxenceng.lazyclaude.shell;

/** A section's dim header, a ticket's id or {@code unlinked}; the cursor skips it. */
record SectionHeaderRow(String label) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.HEADER, label);
    }

    @Override
    public boolean selectable() {
        return false;
    }
}

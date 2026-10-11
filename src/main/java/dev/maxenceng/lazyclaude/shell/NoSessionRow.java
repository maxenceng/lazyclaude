package dev.maxenceng.lazyclaude.shell;

/** What a ticket's section holds when no session is linked to it. */
record NoSessionRow(String ticket) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.NO_SESSION, ticket);
    }
}

package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;

/** A live session. */
record LiveSessionRow(LiveSessionView session) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.SESSION, session.id().value());
    }
}

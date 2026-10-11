package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;

/** A past session. It is keyed like a live one, so the selection stays on a session whose process died. */
record PastSessionRow(PastSessionView session) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.SESSION, session.id().value());
    }
}

package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionView;

/** A hosted session started for the ticket whose section lists it, before the registry knows it. */
record StartingSessionRow(HostedSessionView tab) implements SessionRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.STARTING, tab.id().value().toString());
    }
}

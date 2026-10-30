package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.published.Reason;

/** The only row of a Board whose issues could not be read: why. */
record UnavailableRow(Reason reason) implements Row {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.UNAVAILABLE, reason.value());
    }

    @Override
    public String text() {
        return reason.value();
    }
}

package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.session.published.PlaceName;

/** A place's header, laid out like the Board's status headers, always unfolded; the cursor skips it. */
record ElsewhereHeaderRow(PlaceName place, int count) implements ElsewhereRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.HEADER, place.value());
    }

    @Override
    public boolean selectable() {
        return false;
    }

    @Override
    public RowLine line() {
        return RowLine.plain(new HeaderRow(place.value(), count, false).text());
    }
}

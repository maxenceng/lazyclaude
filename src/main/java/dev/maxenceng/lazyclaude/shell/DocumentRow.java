package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocumentView;

/** A document in the Docs panel: its file name, indented by how many folders it sits in. */
record DocumentRow(DocumentView document, int depth) implements DocsRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.DOCUMENT, document.path().value());
    }

    @Override
    public String text() {
        return INDENT.repeat(depth) + document.path().fileName();
    }
}

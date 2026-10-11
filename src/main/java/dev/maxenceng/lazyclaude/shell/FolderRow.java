package dev.maxenceng.lazyclaude.shell;

/** A folder's header in the Docs panel, indented by how many folders it sits in: its name and the count of documents beneath it. */
record FolderRow(String path, String name, int count, boolean folded, int depth) implements DocsRow {

    @Override
    public RowKey key() {
        return new RowKey(RowKind.FOLDER, path);
    }

    @Override
    public String text() {
        return Indent.STEP.repeat(depth) + new HeaderRow(name, count, folded).text();
    }
}

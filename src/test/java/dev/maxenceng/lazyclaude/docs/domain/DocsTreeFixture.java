package dev.maxenceng.lazyclaude.docs.domain;

import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocumentAt;
import static dev.maxenceng.lazyclaude.docs.domain.UnreadableDocumentFixture.unreadableDocument;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.adrPath;

import java.util.List;

public final class DocsTreeFixture {

    private DocsTreeFixture() {}

    public static DocsTree docsTree() {
        return DocsTree.of(List.of(readDocument(), unreadableDocument(), readDocumentAt(adrPath())));
    }
}

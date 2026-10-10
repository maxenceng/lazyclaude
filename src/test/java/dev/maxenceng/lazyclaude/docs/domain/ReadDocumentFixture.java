package dev.maxenceng.lazyclaude.docs.domain;

import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.documentPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentTextFixture.content;

import dev.maxenceng.lazyclaude.docs.domain.ReadDocument.ReadDocumentBuilder;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;

public final class ReadDocumentFixture {

    private ReadDocumentFixture() {}

    public static ReadDocumentBuilder readDocumentBuilder() {
        return ReadDocument.builder().path(documentPath()).text(content());
    }

    public static ReadDocument readDocument() {
        return readDocumentBuilder().build();
    }

    public static ReadDocument readDocumentAt(DocumentPath path) {
        return readDocumentBuilder().path(path).build();
    }
}

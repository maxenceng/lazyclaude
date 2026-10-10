package dev.maxenceng.lazyclaude.docs.domain;

import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.secondDocumentPath;
import static dev.maxenceng.lazyclaude.docs.published.ReasonFixture.reason;

import dev.maxenceng.lazyclaude.docs.domain.UnreadableDocument.UnreadableDocumentBuilder;

public final class UnreadableDocumentFixture {

    private UnreadableDocumentFixture() {}

    public static UnreadableDocumentBuilder unreadableDocumentBuilder() {
        return UnreadableDocument.builder().path(secondDocumentPath()).reason(reason());
    }

    public static UnreadableDocument unreadableDocument() {
        return unreadableDocumentBuilder().build();
    }
}

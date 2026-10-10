package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import dev.maxenceng.lazyclaude.docs.domain.Document;
import dev.maxenceng.lazyclaude.docs.domain.ReadDocument;
import dev.maxenceng.lazyclaude.docs.domain.UnreadableDocument;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;

/** One document as the shell reads it: its text, or the reason it has none. */
public sealed interface DocumentView permits ReadDocumentView, UnreadableDocumentView {

    DocumentPath path();

    static DocumentView from(Document document) {
        return switch (document) {
            case ReadDocument read -> ReadDocumentView.from(read);
            case UnreadableDocument unreadable -> UnreadableDocumentView.from(unreadable);
        };
    }
}

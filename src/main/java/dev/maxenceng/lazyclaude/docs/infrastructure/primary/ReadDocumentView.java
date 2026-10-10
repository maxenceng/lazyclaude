package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import dev.maxenceng.lazyclaude.docs.domain.ReadDocument;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.DocumentText;

/** A document whose file was read. */
public record ReadDocumentView(DocumentPath path, DocumentText text) implements DocumentView {

    static ReadDocumentView from(ReadDocument document) {
        return builder().path(document.path()).text(document.text()).build();
    }

    public static ReadDocumentViewBuilder builder() {
        return new ReadDocumentViewBuilder();
    }

    public static final class ReadDocumentViewBuilder {
        private DocumentPath path;
        private DocumentText text;

        private ReadDocumentViewBuilder() {}

        public ReadDocumentViewBuilder path(DocumentPath path) {
            this.path = path;
            return this;
        }

        public ReadDocumentViewBuilder text(DocumentText text) {
            this.text = text;
            return this;
        }

        public ReadDocumentView build() {
            return new ReadDocumentView(path, text);
        }
    }
}

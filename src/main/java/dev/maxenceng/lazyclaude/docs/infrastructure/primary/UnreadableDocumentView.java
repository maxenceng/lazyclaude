package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import dev.maxenceng.lazyclaude.docs.domain.UnreadableDocument;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.Reason;

/** A document whose file could not be read, and why. */
public record UnreadableDocumentView(DocumentPath path, Reason reason) implements DocumentView {

    static UnreadableDocumentView from(UnreadableDocument document) {
        return builder().path(document.path()).reason(document.reason()).build();
    }

    public static UnreadableDocumentViewBuilder builder() {
        return new UnreadableDocumentViewBuilder();
    }

    public static final class UnreadableDocumentViewBuilder {
        private DocumentPath path;
        private Reason reason;

        private UnreadableDocumentViewBuilder() {}

        public UnreadableDocumentViewBuilder path(DocumentPath path) {
            this.path = path;
            return this;
        }

        public UnreadableDocumentViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnreadableDocumentView build() {
            return new UnreadableDocumentView(path, reason);
        }
    }
}

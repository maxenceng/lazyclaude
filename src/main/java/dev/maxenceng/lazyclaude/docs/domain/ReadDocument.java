package dev.maxenceng.lazyclaude.docs.domain;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.DocumentText;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A document whose file was read: its text, or the absence of any. */
public record ReadDocument(DocumentPath path, DocumentText text) implements Document {

    public ReadDocument {
        Assert.notNull("path", path);
        Assert.notNull("text", text);
    }

    public static ReadDocumentBuilder builder() {
        return new ReadDocumentBuilder();
    }

    public static final class ReadDocumentBuilder {
        private DocumentPath path;
        private DocumentText text;

        private ReadDocumentBuilder() {}

        public ReadDocumentBuilder path(DocumentPath path) {
            this.path = path;
            return this;
        }

        public ReadDocumentBuilder text(DocumentText text) {
            this.text = text;
            return this;
        }

        public ReadDocument build() {
            return new ReadDocument(path, text);
        }
    }
}

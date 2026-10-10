package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocumentView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.ReadDocumentView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.UnreadableDocumentView;
import dev.maxenceng.lazyclaude.docs.published.Content;
import dev.maxenceng.lazyclaude.docs.published.EmptyDocument;
import dev.tamboui.style.Style;

/** How the shell words a document: its markdown as written, "empty document" for a blank file, the path and the reason for an unreadable one. */
final class DocsText {

    private static final String EMPTY_DOCUMENT = "empty document";

    private DocsText() {}

    static List<DetailRow> head(DocumentView document) {
        return switch (document) {
            case ReadDocumentView read ->
                switch (read.text()) {
                    case Content content -> List.of();
                    case EmptyDocument empty -> List.of(new DetailRow(EMPTY_DOCUMENT, Style.EMPTY.dim()));
                };
            case UnreadableDocumentView unreadable ->
                List.of(
                        new DetailRow(unreadable.path().value(), Style.EMPTY.bold()),
                        new DetailRow(unreadable.reason().value(), Style.EMPTY));
        };
    }

    static Optional<String> markdown(DocumentView document) {
        return switch (document) {
            case ReadDocumentView read ->
                switch (read.text()) {
                    case Content content -> Optional.of(content.value());
                    case EmptyDocument empty -> Optional.empty();
                };
            case UnreadableDocumentView unreadable -> Optional.empty();
        };
    }
}

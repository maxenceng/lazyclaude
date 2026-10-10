package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.adrPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.deepPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.documentPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.researchPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.secondAdrPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.secondDocumentPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentTextFixture.content;
import static dev.maxenceng.lazyclaude.docs.published.DocumentTextFixture.secondContent;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.adr;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.research;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.superseded;
import static dev.maxenceng.lazyclaude.docs.published.ReasonFixture.reason;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.docs.published.Content;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.DocumentText;
import dev.maxenceng.lazyclaude.docs.published.EmptyDocument;
import dev.maxenceng.lazyclaude.docs.published.FolderName;

public final class DocsViewFixture {

    private static final FolderName DOCS = new FolderName("docs");

    private DocsViewFixture() {}

    public static ReadDocumentView documentView() {
        return documentViewAt(documentPath(), content());
    }

    public static ReadDocumentView secondDocumentView() {
        return documentViewAt(secondDocumentPath(), secondContent());
    }

    public static ReadDocumentView documentViewAt(DocumentPath path, DocumentText text) {
        return ReadDocumentView.builder().path(path).text(text).build();
    }

    public static UnreadableDocumentView unreadableDocumentView() {
        return UnreadableDocumentView.builder().path(secondDocumentPath()).reason(reason()).build();
    }

    public static FolderView adrFolderView() {
        return folderView(adr(), List.of(documentViewAt(adrPath(), content()), documentViewAt(secondAdrPath(), content())), List.of());
    }

    public static FolderView folderView(FolderName name, List<DocumentView> documents, List<FolderView> folders) {
        return FolderView.builder().name(name).documents(documents).folders(folders).build();
    }

    public static DocsView docsViewOf(List<DocumentView> documents, List<FolderView> folders) {
        return DocsView.builder().root(folderView(DOCS, documents, folders)).build();
    }

    public static DocsView emptyDocsView() {
        return docsViewOf(List.of(), List.of());
    }

    public static FolderView researchFolderView() {
        return folderView(research(), List.of(documentViewAt(researchPath(), content())), List.of());
    }

    public static DocsView docsView() {
        return docsViewOf(List.of(documentView(), secondDocumentView()), List.of(adrFolderView(), researchFolderView()));
    }

    public static DocsView nestedDocsView() {
        FolderView superseded = folderView(superseded(), List.of(documentViewAt(deepPath(), content())), List.of());
        FolderView adrWithOld = folderView(adr(), List.of(documentViewAt(adrPath(), content())), List.of(superseded));
        return docsViewOf(List.of(), List.of(adrWithOld));
    }

    public static ReadDocumentView emptyDocumentView() {
        return documentViewAt(new DocumentPath("empty.md"), new EmptyDocument());
    }

    public static ReadDocumentView longDocumentView() {
        return documentViewAt(documentPath(), listOfRows(60));
    }

    public static ReadDocumentView longEditedDocumentView() {
        return documentViewAt(documentPath(), listOfRows(70));
    }

    public static DocsView docsViewOfDocuments(DocumentView... documents) {
        return docsViewOf(List.of(documents), List.of());
    }

    public static ReadDocumentView namedDocumentView(String fileName) {
        return documentViewAt(new DocumentPath(fileName), content());
    }

    private static Content listOfRows(int count) {
        return new Content(IntStream.rangeClosed(1, count)
                .mapToObj(number -> "- Row %02d".formatted(number))
                .collect(Collectors.joining("\n")));
    }
}

package dev.maxenceng.lazyclaude.docs.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.FolderPath;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * A project's documents grouped by folder: the files directly under {@code docs/}, then its folders.
 * Its rules: only listed documents are in it (markdown, outside {@code docs/backlog/}), a folder with
 * none beneath it is not in it, and a folder holds its own files apart from its folders. It keeps the
 * order the documents were listed in, so a folder comes where its first document did.
 */
public record DocsTree(List<Document> documents, List<Folder> folders) implements Docs {

    public DocsTree {
        Assert.notNull("documents", documents);
        Assert.notNull("folders", folders);
    }

    public static DocsTree of(List<Document> documents) {
        List<Document> listed =
                documents.stream().filter(document -> document.path().listed()).toList();
        return builder()
                .documents(directlyIn(List.of(), listed))
                .folders(foldersIn(List.of(), listed))
                .build();
    }

    private static List<Document> directlyIn(List<String> folder, List<Document> beneath) {
        return beneath.stream()
                .filter(document -> document.path().segments().size() == folder.size() + 1)
                .toList();
    }

    private static List<Folder> foldersIn(List<String> folder, List<Document> beneath) {
        return beneath.stream()
                .map(document -> document.path().segments())
                .filter(segments -> segments.size() > folder.size() + 1)
                .map(segments -> segments.subList(0, folder.size() + 1))
                .distinct()
                .map(child -> folderAt(child, beneath))
                .toList();
    }

    private static Folder folderAt(List<String> folder, List<Document> listed) {
        List<Document> beneath = listed.stream()
                .filter(document -> isBeneath(document, folder))
                .toList();
        return Folder.builder()
                .path(FolderPath.of(folder))
                .documents(directlyIn(folder, beneath))
                .folders(foldersIn(folder, beneath))
                .build();
    }

    private static boolean isBeneath(Document document, List<String> folder) {
        List<String> segments = document.path().segments();
        return segments.size() > folder.size() && segments.subList(0, folder.size()).equals(folder);
    }

    static DocsTreeBuilder builder() {
        return new DocsTreeBuilder();
    }

    public static final class DocsTreeBuilder {
        private List<Document> documents;
        private List<Folder> folders;

        private DocsTreeBuilder() {}

        public DocsTreeBuilder documents(List<Document> documents) {
            this.documents = documents;
            return this;
        }

        public DocsTreeBuilder folders(List<Folder> folders) {
            this.folders = folders;
            return this;
        }

        public DocsTree build() {
            return new DocsTree(documents, folders);
        }
    }
}

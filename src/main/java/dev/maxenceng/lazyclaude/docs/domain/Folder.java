package dev.maxenceng.lazyclaude.docs.domain;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.docs.published.FolderName;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A folder of the docs tree: the documents directly in it by name, then its folders by name, whatever order they came in. */
public record Folder(FolderName name, List<Document> documents, List<Folder> folders) {

    public Folder {
        Assert.notNull("name", name);
        Assert.notNull("documents", documents);
        Assert.notNull("folders", folders);
        documents = documents.stream().sorted(Comparator.comparing(Document::path)).toList();
        folders = folders.stream().sorted(Comparator.comparing(Folder::name)).toList();
    }

    /** The folder {@code name} at {@code path}, made of the {@code listed} documents that are beneath that path. */
    static Folder at(FolderName name, List<String> path, List<Document> listed) {
        List<Document> beneath = listed.stream().filter(document -> isBeneath(document, path)).toList();
        return builder()
                .name(name)
                .documents(beneath.stream()
                        .filter(document -> document.path().segments().size() == path.size() + 1)
                        .toList())
                .folders(beneath.stream()
                        .map(document -> document.path().segments())
                        .filter(segments -> segments.size() > path.size() + 1)
                        .map(segments -> segments.get(path.size()))
                        .distinct()
                        .map(folder -> Folder.at(new FolderName(folder), child(path, folder), beneath))
                        .toList())
                .build();
    }

    private static boolean isBeneath(Document document, List<String> path) {
        List<String> segments = document.path().segments();
        return segments.size() > path.size() && segments.subList(0, path.size()).equals(path);
    }

    private static List<String> child(List<String> path, String folder) {
        return Stream.concat(path.stream(), Stream.of(folder)).toList();
    }

    static FolderBuilder builder() {
        return new FolderBuilder();
    }

    public static final class FolderBuilder {
        private FolderName name;
        private List<Document> documents;
        private List<Folder> folders;

        private FolderBuilder() {}

        public FolderBuilder name(FolderName name) {
            this.name = name;
            return this;
        }

        public FolderBuilder documents(List<Document> documents) {
            this.documents = documents;
            return this;
        }

        public FolderBuilder folders(List<Folder> folders) {
            this.folders = folders;
            return this;
        }

        public Folder build() {
            return new Folder(name, documents, folders);
        }
    }
}

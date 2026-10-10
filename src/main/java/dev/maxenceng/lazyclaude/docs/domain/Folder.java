package dev.maxenceng.lazyclaude.docs.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.FolderName;
import dev.maxenceng.lazyclaude.docs.published.FolderPath;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A folder of the docs tree: the documents directly in it, then its folders, in the order they were listed. */
public record Folder(FolderPath path, List<Document> documents, List<Folder> folders) {

    public Folder {
        Assert.notNull("path", path);
        Assert.notNull("documents", documents);
        Assert.notNull("folders", folders);
    }

    public FolderName name() {
        return path.name();
    }

    static FolderBuilder builder() {
        return new FolderBuilder();
    }

    public static final class FolderBuilder {
        private FolderPath path;
        private List<Document> documents;
        private List<Folder> folders;

        private FolderBuilder() {}

        public FolderBuilder path(FolderPath path) {
            this.path = path;
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
            return new Folder(path, documents, folders);
        }
    }
}

package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.domain.Folder;
import dev.maxenceng.lazyclaude.docs.published.FolderName;

/** One folder: its documents by name, then its folders by name. */
public record FolderView(FolderName name, List<DocumentView> documents, List<FolderView> folders) {

    static FolderView from(Folder folder) {
        return builder()
                .name(folder.name())
                .documents(folder.documents().stream().map(DocumentView::from).toList())
                .folders(folder.folders().stream().map(FolderView::from).toList())
                .build();
    }

    /** The documents beneath it, at any depth: what its header counts. */
    public int count() {
        return documents.size() + folders.stream().mapToInt(FolderView::count).sum();
    }

    public static FolderViewBuilder builder() {
        return new FolderViewBuilder();
    }

    public static final class FolderViewBuilder {
        private FolderName name;
        private List<DocumentView> documents;
        private List<FolderView> folders;

        private FolderViewBuilder() {}

        public FolderViewBuilder name(FolderName name) {
            this.name = name;
            return this;
        }

        public FolderViewBuilder documents(List<DocumentView> documents) {
            this.documents = documents;
            return this;
        }

        public FolderViewBuilder folders(List<FolderView> folders) {
            this.folders = folders;
            return this;
        }

        public FolderView build() {
            return new FolderView(name, documents, folders);
        }
    }
}

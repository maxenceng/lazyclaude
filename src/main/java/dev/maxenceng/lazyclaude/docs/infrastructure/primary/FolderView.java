package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.domain.Folder;
import dev.maxenceng.lazyclaude.docs.published.FolderName;
import dev.maxenceng.lazyclaude.docs.published.FolderPath;

/** One folder, known by its path: its documents, then its folders. */
public record FolderView(FolderPath path, List<DocumentView> documents, List<FolderView> folders) {

    static FolderView from(Folder folder) {
        return builder()
                .path(folder.path())
                .documents(folder.documents().stream().map(DocumentView::from).toList())
                .folders(folder.folders().stream().map(FolderView::from).toList())
                .build();
    }

    public FolderName name() {
        return path.name();
    }

    /** The documents beneath it, at any depth: what its header counts. */
    public int count() {
        return documents.size() + folders.stream().mapToInt(FolderView::count).sum();
    }

    public static FolderViewBuilder builder() {
        return new FolderViewBuilder();
    }

    public static final class FolderViewBuilder {
        private FolderPath path;
        private List<DocumentView> documents;
        private List<FolderView> folders;

        private FolderViewBuilder() {}

        public FolderViewBuilder path(FolderPath path) {
            this.path = path;
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
            return new FolderView(path, documents, folders);
        }
    }
}

package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.domain.DocsTree;

/** The docs tree: the files directly under {@code docs/} first, then its folders. */
public record DocsTreeView(List<DocumentView> documents, List<FolderView> folders) implements DocsView {

    static DocsTreeView from(DocsTree tree) {
        return builder()
                .documents(tree.documents().stream().map(DocumentView::from).toList())
                .folders(tree.folders().stream().map(FolderView::from).toList())
                .build();
    }

    public static DocsTreeViewBuilder builder() {
        return new DocsTreeViewBuilder();
    }

    public static final class DocsTreeViewBuilder {
        private List<DocumentView> documents;
        private List<FolderView> folders;

        private DocsTreeViewBuilder() {}

        public DocsTreeViewBuilder documents(List<DocumentView> documents) {
            this.documents = documents;
            return this;
        }

        public DocsTreeViewBuilder folders(List<FolderView> folders) {
            this.folders = folders;
            return this;
        }

        public DocsTreeView build() {
            return new DocsTreeView(documents, folders);
        }
    }
}

package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import dev.maxenceng.lazyclaude.docs.domain.DocsTree;

/** The docs tree as the shell reads it: the folder {@code docs/} itself, its files first, then its folders. */
public record DocsView(FolderView root) {

    static DocsView from(DocsTree tree) {
        return builder().root(FolderView.from(tree.root())).build();
    }

    public static DocsViewBuilder builder() {
        return new DocsViewBuilder();
    }

    public static final class DocsViewBuilder {
        private FolderView root;

        private DocsViewBuilder() {}

        public DocsViewBuilder root(FolderView root) {
            this.root = root;
            return this;
        }

        public DocsView build() {
            return new DocsView(root);
        }
    }
}

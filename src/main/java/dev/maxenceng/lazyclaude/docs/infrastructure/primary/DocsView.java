package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import dev.maxenceng.lazyclaude.docs.domain.Docs;
import dev.maxenceng.lazyclaude.docs.domain.DocsTree;
import dev.maxenceng.lazyclaude.docs.domain.UnavailableDocs;

/** A project's docs as the shell reads them: the tree, or why there is none. */
public sealed interface DocsView permits DocsTreeView, UnavailableDocsView {

    static DocsView from(Docs docs) {
        return switch (docs) {
            case DocsTree tree -> DocsTreeView.from(tree);
            case UnavailableDocs unavailable -> UnavailableDocsView.from(unavailable);
        };
    }
}

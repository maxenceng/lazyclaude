package dev.maxenceng.lazyclaude.docs.domain;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Builds the docs tree of a project. Only what the tree lists is read, so a backlog or an image is
 * never opened for a row that will not be shown.
 */
public record DocsManager(DocsPort docs, DocsWatchPort watch) {

    public DocsTree docsFor(Project project) {
        return DocsTree.of(docs.paths(project).stream()
                .filter(DocumentPath::listed)
                .map(path -> docs.read(project, path))
                .toList());
    }

    public void watch(Project project, Runnable changed) {
        watch.watch(project, changed);
    }
}

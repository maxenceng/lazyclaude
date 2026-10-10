package dev.maxenceng.lazyclaude.docs.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Builds the docs tree of a project, or says why it cannot. Only what the tree lists is read, so a
 * backlog or an image is never opened for a row that will not be shown.
 */
public record DocsManager(DocsPort docs, DocsWatchPort watch) {

    public Docs docsFor(Project project) {
        List<DocumentPath> paths;
        try {
            paths = docs.paths(project);
        } catch (DocsUnavailableException unavailable) {
            return new UnavailableDocs(new Reason(unavailable.getMessage()));
        }
        return DocsTree.of(paths.stream()
                .filter(DocumentPath::listed)
                .map(path -> docs.read(project, path))
                .toList());
    }

    public void watch(Project project, Runnable changed) {
        watch.watch(project, changed);
    }
}

package dev.maxenceng.lazyclaude.docs.domain;

import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Builds the docs tree of a project, or says why it cannot. Only what the tree lists is read, so a
 * backlog or an image is never opened for a row that will not be shown.
 */
public record DocsManager(DocsPort docs, DocsWatchPort watch, WarningPort warnings) {

    private static final String POLLING_WARNING =
            "docs: %s; checking every %d s instead. If the cause is the watch limit, raise fs.inotify.max_user_watches.";

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
        watch.watch(project, changed, this::warnOfPolling);
    }

    private void warnOfPolling(String cause, Duration every) {
        warnings.raise(new Warning(POLLING_WARNING.formatted(cause, every.toSeconds())));
    }
}

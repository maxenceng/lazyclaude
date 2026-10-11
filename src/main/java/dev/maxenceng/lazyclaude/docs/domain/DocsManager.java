package dev.maxenceng.lazyclaude.docs.domain;

import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Builds the docs tree of a project, or says why it cannot. Only what the tree lists is read, so a
 * backlog or an image is never opened for a row that will not be shown. Falling back to polling is
 * warned about once, however often the watcher reports it.
 */
public final class DocsManager {

    private static final String POLLING_WARNING = "docs: %s; checking every %d s instead.";

    private final DocsPort docs;
    private final DocsWatchPort watch;
    private final WarningPort warnings;
    private final WarnOnce polling = new WarnOnce();

    public DocsManager(DocsPort docs, DocsWatchPort watch, WarningPort warnings) {
        this.docs = docs;
        this.watch = watch;
        this.warnings = warnings;
    }

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

    private void warnOfPolling(Reason cause, Duration every) {
        polling.raise(warnings, new Warning(POLLING_WARNING.formatted(cause.value(), every.toSeconds())));
    }
}

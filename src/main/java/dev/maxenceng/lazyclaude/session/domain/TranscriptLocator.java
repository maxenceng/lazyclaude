package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.shared.domain.ProjectUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** How the domain finds the project of a transcript's {@code cwd}; the watcher asks it once per new transcript. */
@FunctionalInterface
public interface TranscriptLocator {

    /** @throws ProjectUnavailableException when the lookup itself failed, so the transcript is asked again next time */
    ProjectLocation locate(WorkingDirectory directory);
}

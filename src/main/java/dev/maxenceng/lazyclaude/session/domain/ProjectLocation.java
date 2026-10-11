package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Where a transcript's last {@code cwd} was found to belong when the transcript was first scanned. */
public sealed interface ProjectLocation permits LocatedInProject, NotLocated {

    /** Whether a past session working in {@code directory}, located like this, is {@code project}'s. */
    boolean holds(Project project, WorkingDirectory directory);
}

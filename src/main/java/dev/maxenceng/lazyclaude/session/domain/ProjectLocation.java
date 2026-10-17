package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Where a session's {@code cwd} was found to belong: a live session's on each read of the registry, a transcript's last
 * {@code cwd} once, when the transcript was first scanned.
 */
public sealed interface ProjectLocation permits LocatedInProject, NotLocated {

    /** Whether a session working in {@code directory}, located like this, is {@code project}'s. */
    boolean holds(Project project, WorkingDirectory directory);

    /** Where a session working in {@code directory}, located like this, belongs when {@code project} is the one lazyclaude works on. */
    SessionPlace placeOf(Project project, WorkingDirectory directory);
}

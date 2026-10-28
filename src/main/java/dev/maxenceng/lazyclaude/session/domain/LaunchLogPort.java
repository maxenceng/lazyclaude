package dev.maxenceng.lazyclaude.session.domain;

import java.util.function.UnaryOperator;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where the launch log of a project is kept: the sessions lazyclaude started in it (ADR 0047). */
public interface LaunchLogPort {

    /**
     * The log of {@code project}; empty while none was ever written, and empty when it cannot be read or does not parse, in
     * which case {@code fallback} is told why, the first time anything about the log fails.
     */
    LaunchLog read(Project project, LaunchLogFallback fallback);

    /**
     * Replaces the log of {@code project} with {@code change} applied to it, one change at a time, so a launch, an end and a
     * prune never overwrite each other. A log that cannot be read is left as it is, and one that cannot be written changes
     * nothing; either way {@code fallback} is told why, the first time anything about the log fails.
     */
    void change(Project project, LaunchLogFallback fallback, UnaryOperator<LaunchLog> change);
}

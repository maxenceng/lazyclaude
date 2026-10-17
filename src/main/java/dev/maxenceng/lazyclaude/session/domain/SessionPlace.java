package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.PlaceName;

/** Where a live session belongs: the project its {@code cwd} is in, or, outside git, its own {@code cwd}. */
public sealed interface SessionPlace permits ProjectPlace, OutsideGitPlace {

    PlaceName name();
}

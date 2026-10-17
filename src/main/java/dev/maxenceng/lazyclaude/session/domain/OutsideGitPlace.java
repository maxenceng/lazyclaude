package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.PlaceName;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Sessions whose {@code cwd} is in no git repository: each directory is a place of its own. */
public record OutsideGitPlace(WorkingDirectory directory) implements SessionPlace {

    public OutsideGitPlace {
        Assert.notNull("directory", directory);
    }

    @Override
    public PlaceName name() {
        return new PlaceName(directory.path().toString());
    }
}

package dev.maxenceng.lazyclaude.session.domain;

import java.nio.file.Path;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.PlaceName;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** A session in a project, a ticket worktree's included. */
public record ProjectPlace(Project project) implements SessionPlace {

    public ProjectPlace {
        Assert.notNull("project", project);
    }

    @Override
    public PlaceName name() {
        Path root = project.root();
        return new PlaceName(Optional.ofNullable(root.getFileName()).orElse(root).toString());
    }
}

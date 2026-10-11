package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Git located the {@code cwd} in this project. */
public record LocatedInProject(Project project) implements ProjectLocation {

    public LocatedInProject {
        Assert.notNull("project", project);
    }

    @Override
    public boolean holds(Project other, WorkingDirectory directory) {
        return project.equals(other);
    }
}

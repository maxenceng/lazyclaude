package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** What {@link HostedSessionCommand} is asked a new unlinked session with: the project root it runs in, and the size of the pane it is drawn in. */
public record NewHostedPayload(Path root, PaneSize size) {

    public NewHostedPayload {
        Assert.notNull("pane size", size);
    }

    Project toDomain() {
        return new Project(root);
    }
}

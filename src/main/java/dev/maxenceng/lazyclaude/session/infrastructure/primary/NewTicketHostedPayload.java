package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.TicketDescription;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * What {@link HostedSessionCommand} is asked a session that creates a ticket with: the project root it runs in, the description
 * as typed and the size of the pane it is drawn in.
 */
public record NewTicketHostedPayload(Path root, String description, PaneSize size) {

    public NewTicketHostedPayload {
        Assert.notBlank("description", description);
        Assert.notNull("pane size", size);
    }

    Project project() {
        return new Project(root);
    }

    TicketDescription toDomain() {
        return new TicketDescription(description);
    }
}

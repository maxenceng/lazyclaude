package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * What {@link HostedSessionCommand} is asked a resume of a past session with: the project's sessions as they are asked for, the
 * session, the directory it was launched in, what its row is listed under, and the pane's size.
 */
public record ResumePastPayload(
        SessionPayload sessions, SessionId session, Path recordedDirectory, ListingPayload listing, PaneSize size) {

    public ResumePastPayload {
        Assert.notNull("sessions", sessions);
        Assert.notNull("session", session);
        Assert.notNull("listing", listing);
        Assert.notNull("pane size", size);
    }

    WorkingDirectory recorded() {
        return new WorkingDirectory(recordedDirectory);
    }
}

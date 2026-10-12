package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** What {@link HostedSessionCommand} is asked a resume with: the past session, the directory its transcript recorded, and the pane's size. */
public record ResumeHostedPayload(SessionId session, Path recordedDirectory, PaneSize size) {

    public ResumeHostedPayload {
        Assert.notNull("session", session);
        Assert.notNull("pane size", size);
    }

    WorkingDirectory recorded() {
        return new WorkingDirectory(recordedDirectory);
    }
}

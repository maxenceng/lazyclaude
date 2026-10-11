package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.typedA;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;

import dev.maxenceng.lazyclaude.session.published.HostedSessionId;

public final class HostedSessionPayloadFixture {

    private HostedSessionPayloadFixture() {}

    public static NewHostedPayload newHostedPayload() {
        return new NewHostedPayload(project().root(), paneSize());
    }

    public static ResumeHostedPayload resumeHostedPayload() {
        return new ResumeHostedPayload(project().root(), sessionId(), worktreeDirectory().path(), paneSize());
    }

    public static HostedInputPayload typingPayload(HostedSessionId id) {
        return new HostedInputPayload(id, typedA());
    }

    public static ResizePayload resizePayload(HostedSessionId id) {
        return new ResizePayload(id, resizedPaneSize());
    }
}

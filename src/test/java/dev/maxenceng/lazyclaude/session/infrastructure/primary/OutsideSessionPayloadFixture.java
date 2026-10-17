package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import java.util.List;

import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;

public final class OutsideSessionPayloadFixture {

    private OutsideSessionPayloadFixture() {}

    public static JumpPayload jumpPayload() {
        return new JumpPayload(new SessionPayload(project().root(), List.of(linkedTicketId())), sessionId());
    }

    public static TranscriptPayload transcriptPayload() {
        return new TranscriptPayload(sessionId(), projectRootDirectory().path(), TranscriptOffset.start());
    }
}

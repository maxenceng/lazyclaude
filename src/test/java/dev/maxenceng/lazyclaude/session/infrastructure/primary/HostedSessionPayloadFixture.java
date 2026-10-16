package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TerminalInputFixture.typedA;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;

public final class HostedSessionPayloadFixture {

    private HostedSessionPayloadFixture() {}

    public static NewHostedPayload newHostedPayload() {
        return new NewHostedPayload(project().root(), paneSize());
    }

    public static ResumeHostedPayload resumeHostedPayload() {
        return new ResumeHostedPayload(sessionId(), worktreeDirectory().path(), paneSize());
    }

    public static HostedInputPayload typingPayload(HostedSessionId id) {
        return new HostedInputPayload(id, typedA());
    }

    public static ResizePayload resizePayload(HostedSessionId id) {
        return new ResizePayload(id, resizedPaneSize());
    }

    public static TicketHostedPayload ticketHostedPayload(Verb verb) {
        return new TicketHostedPayload(project().root(), linkedTicketId(), branchName().value(), verb, paneSize());
    }

    public static ResumeTicketHostedPayload resumeTicketHostedPayload() {
        return new ResumeTicketHostedPayload(
                sessionId(), worktreeDirectory().path(), project().root(), branchName().value(), paneSize());
    }

    public static VerbChoicePayload verbChoicePayload(List<Verb> verbs, boolean merged) {
        return new VerbChoicePayload(
                new SessionPayload(project().root(), List.of(linkedTicketId())), linkedTicketId(), verbs, merged);
    }
}

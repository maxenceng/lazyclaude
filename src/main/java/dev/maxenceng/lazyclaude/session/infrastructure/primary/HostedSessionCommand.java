package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.published.HostStart;

/** What the shell asks of {@code session} when it changes something: start, resume, feed, resize and end hosted sessions. */
public final class HostedSessionCommand {

    private final SessionApplicationService sessions;

    public HostedSessionCommand(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** A plain {@code claude} in the project root; a failure to start it is already a warning, and the result says none started. */
    public HostStart startUnlinked(NewHostedPayload payload) {
        return sessions.startUnlinked(payload.toDomain(), payload.size());
    }

    /** {@code claude --resume} of a past session, in its recorded directory, or the project root with a warning when that is gone. */
    public HostStart resume(ResumeHostedPayload payload) {
        return sessions.resume(payload.toDomain(), payload.session(), payload.recorded(), payload.size());
    }

    public void send(HostedInputPayload payload) {
        sessions.send(payload.session(), payload.input());
    }

    public void resize(ResizePayload payload) {
        sessions.resize(payload.session(), payload.size());
    }

    /** Ends every hosted {@code claude}, as lazyclaude quits. */
    public void endAll() {
        sessions.endAll();
    }
}

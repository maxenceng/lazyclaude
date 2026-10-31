package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.published.HostStart;

/** What the shell asks of {@code session} when it changes something: start, resume and end hosted sessions. */
public final class HostedSessionCommand {

    private final SessionApplicationService sessions;

    public HostedSessionCommand(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** A plain {@code claude} in the project root; a failure to start it is already a warning, and the result says none started. */
    public HostStart startUnlinked(NewHostedPayload payload) {
        return sessions.startUnlinked(payload.toDomain(), payload.size());
    }

    /** {@code /ticket <verb> <ID>} in the ticket's worktree, added first when it is missing; a failure to start it is already a warning. */
    public HostStart startTicket(TicketHostedPayload payload) {
        return sessions.startTicket(payload.toDomain(), payload.size());
    }

    /** {@code claude --resume} of a ticket's past session, its worktree added back first when that is the recorded directory and is gone. */
    public HostStart resumeInTicketWorktree(ResumeTicketHostedPayload payload) {
        return sessions.resumeInTicketWorktree(payload.session(), payload.recorded(), payload.worktree(), payload.size());
    }

    /** {@code claude --resume} of a past session, in the directory it was launched in, or nothing with a warning when that is gone. */
    public HostStart resume(ResumeHostedPayload payload) {
        return sessions.resume(payload.project(), payload.session(), payload.recorded(), payload.size());
    }

    /** Writes the bytes to the session's pty as they are; nothing for a session that is gone. */
    public void write(PtyInputPayload payload) {
        sessions.write(payload.session(), payload.data());
    }

    /** Gives the session's pty a new size, for a frontend that has already resized its own emulator. */
    public void resizePty(PtyResizePayload payload) {
        sessions.resize(payload.session(), payload.size());
    }

    /** Has the session's {@code claude} draw its whole screen again, for a frontend whose emulator starts empty (ADR 0049). */
    public void repaint(RepaintPayload payload) {
        sessions.repaint(payload.session());
    }

    /** Ends every hosted {@code claude}, as lazyclaude quits. */
    public void endAll() {
        sessions.endAll();
    }
}

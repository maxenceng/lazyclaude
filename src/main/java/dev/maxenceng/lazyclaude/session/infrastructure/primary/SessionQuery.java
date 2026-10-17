package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.WorktreeUnreadableException;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;

/** What the shell asks of {@code session}: a project's live and past sessions as a view, and a view again whenever they may have changed. It only reads: the hosted sessions' screens included. */
public final class SessionQuery {

    private final SessionApplicationService sessions;

    public SessionQuery(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** The project's live and past sessions, or why none could be read; never a failure. */
    public SessionsView sessionsFor(SessionPayload payload) {
        return SessionsView.from(sessions.sessionsFor(payload.toDomain(), payload.tickets()));
    }

    /** The verbs a ticket allows and the one to start on; the sessions are the last pushed, read afresh only before the first push. */
    public VerbChoiceView verbChoiceFor(VerbChoicePayload payload) {
        return VerbChoiceView.from(sessions.verbChoiceFor(
                payload.sessions().toDomain(),
                payload.sessions().tickets(),
                payload.ticket(),
                payload.verbs(),
                payload.merged()));
    }

    /** Hands {@code listener} a view on each change and each sweep, on a thread of the watcher's own, not the caller's. */
    public void subscribe(SessionPayload payload, Consumer<SessionsView> listener) {
        sessions.subscribe(payload.toDomain(), payload.tickets(), read -> listener.accept(SessionsView.from(read)));
    }

    /** Reads the live sessions and the transcripts again now, linked against the tickets of {@code payload}, and hands the subscriber a view when they land. */
    public void refresh(SessionPayload payload) {
        sessions.refresh(payload.toDomain(), payload.tickets());
    }

    /** From now on sessions are linked against the tickets of {@code payload}, and the subscriber is handed a view linked that way at once. */
    public void knowTickets(SessionPayload payload) {
        sessions.knowTickets(payload.toDomain(), payload.tickets());
    }

    /**
     * The ticket's worktree and what stands in it, none when it has none, or why that could not be told; never a failure. The sessions
     * are the last pushed, as for {@link #verbChoiceFor}. It runs {@code git}, so not on the render thread.
     */
    public WorktreeView worktreeOf(WorktreePayload payload) {
        try {
            return sessions.worktreeOf(
                            payload.sessions().toDomain(),
                            payload.sessions().tickets(),
                            payload.toDomain(),
                            payload.mergedHead())
                    .<WorktreeView>map(HeldWorktreeView::from)
                    .orElseGet(NoWorktreeView::new);
        } catch (WorktreeUnavailableException | WorktreeUnreadableException unreadable) {
            return new UnreadableWorktreeView(new Reason(unreadable.getMessage()));
        }
    }

    /** Hands {@code listener} the ticket whose {@code /ticket done} session ended, however it ended, on whichever thread ended it. */
    public void onTicketDoneEnded(Consumer<LinkedTicketId> listener) {
        sessions.onTicketDoneEnded(listener::accept);
    }

    /** What the session's transcript gained since {@code payload}'s offset; never a failure. Reads a file, so not on the render thread. */
    public TranscriptView transcriptOf(TranscriptPayload payload) {
        return TranscriptView.from(
                sessions.transcriptOf(payload.session(), payload.workingDirectory(), payload.offset()));
    }

    /** What a hosted session shows right now, blank for one that is gone. */
    public TerminalScreen screenOf(HostedSessionId id) {
        return sessions.screenOf(id);
    }

    /** Tells {@code listener} whenever a hosted session drew something, on a thread of the host's own. */
    public void onScreenChange(Runnable listener) {
        sessions.onScreenChange(listener);
    }
}

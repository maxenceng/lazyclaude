package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyOutputView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RepaintPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeTicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionExitView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketHostedPayload;
import dev.maxenceng.lazyclaude.session.published.HostStart;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

/**
 * The {@code session/} and {@code pty/} methods: one per operation of {@link SessionQuery} and {@link HostedSessionCommand}, named
 * after it, except that the pty's bytes travel as {@code pty/input}, {@code pty/resize} and {@code pty/output} (ADR 0050). There is
 * no method to end a session: closing stdin ends every one.
 */
final class SessionMethods {

    private static final ProtocolMethod<NewHostedPayload, HostStart> START_UNLINKED =
            new ProtocolMethod<>("session/startUnlinked", NewHostedPayload.class, HostStart.class);
    private static final ProtocolMethod<TicketHostedPayload, HostStart> START_TICKET =
            new ProtocolMethod<>("session/startTicket", TicketHostedPayload.class, HostStart.class);
    private static final ProtocolMethod<ResumeHostedPayload, HostStart> RESUME =
            new ProtocolMethod<>("session/resume", ResumeHostedPayload.class, HostStart.class);
    private static final ProtocolMethod<ResumeTicketHostedPayload, HostStart> RESUME_IN_TICKET_WORKTREE =
            new ProtocolMethod<>("session/resumeInTicketWorktree", ResumeTicketHostedPayload.class, HostStart.class);
    private static final ProtocolMethod<SessionPayload, SessionsView> WATCH =
            new ProtocolMethod<>("session/watch", SessionPayload.class, SessionsView.class);
    private static final ProtocolInput<PtyInputPayload> PTY_INPUT = new ProtocolInput<>("pty/input", PtyInputPayload.class);
    private static final ProtocolInput<PtyResizePayload> PTY_RESIZE = new ProtocolInput<>("pty/resize", PtyResizePayload.class);
    private static final ProtocolInput<RepaintPayload> REPAINT = new ProtocolInput<>("session/repaint", RepaintPayload.class);
    private static final ProtocolNotification<SessionsView> CHANGED = new ProtocolNotification<>("session/changed", SessionsView.class);
    private static final ProtocolNotification<PtyOutputView> OUTPUT = new ProtocolNotification<>("pty/output", PtyOutputView.class);
    private static final ProtocolNotification<SessionExitView> EXITED = new ProtocolNotification<>("session/exited", SessionExitView.class);

    private final SessionQuery sessions;
    private final HostedSessionCommand hosting;
    private final SidecarOutput output;
    private final Set<Path> watching = ConcurrentHashMap.newKeySet();

    SessionMethods(SessionQuery sessions, HostedSessionCommand hosting, SidecarOutput output) {
        this.sessions = sessions;
        this.hosting = hosting;
        this.output = output;
    }

    static List<ProtocolMethod<?, ?>> protocolMethods() {
        return List.of(START_UNLINKED, START_TICKET, RESUME, RESUME_IN_TICKET_WORKTREE, WATCH);
    }

    static List<ProtocolInput<?>> protocolInputs() {
        return List.of(PTY_INPUT, PTY_RESIZE, REPAINT);
    }

    static List<ProtocolNotification<?>> protocolNotifications() {
        return List.of(CHANGED, OUTPUT, EXITED);
    }

    /** The methods, and the pushes of what a hosted {@code claude} writes and how it exits wired to the output. */
    Map<String, SidecarMethod> methods() {
        sessions.onOutput(view -> output.send(OUTPUT.message(view)));
        sessions.onExit(view -> output.send(EXITED.message(view)));
        return Map.of(
                START_UNLINKED.name(), params -> answered(START_UNLINKED.answer(params, hosting::startUnlinked)),
                START_TICKET.name(), params -> answered(START_TICKET.answer(params, hosting::startTicket)),
                RESUME.name(), params -> answered(RESUME.answer(params, hosting::resume)),
                RESUME_IN_TICKET_WORKTREE.name(), params -> answered(RESUME_IN_TICKET_WORKTREE.answer(params, hosting::resumeInTicketWorktree)),
                WATCH.name(), this::watch,
                PTY_INPUT.name(), params -> taken(params, PTY_INPUT, hosting::write),
                PTY_RESIZE.name(), params -> taken(params, PTY_RESIZE, hosting::resizePty),
                REPAINT.name(), params -> taken(params, REPAINT, hosting::repaint));
    }

    private static SidecarAnswer answered(HostStart start) {
        return SidecarAnswer.of(SidecarJson.tree(start));
    }

    private static <P> SidecarAnswer taken(JsonNode params, ProtocolInput<P> input, Consumer<P> operation) {
        input.accept(params, operation);
        return SidecarAnswer.of(NullNode.getInstance());
    }

    /** The watch opens once the current sessions are queued, so no {@code session/changed} reaches the frontend before the sessions it changes. */
    // deferred: a change between this read and the watch starting is pushed only at the next change — close it when a session started as the frontend connects is seen stale.
    private SidecarAnswer watch(JsonNode params) {
        SessionsView current = WATCH.answer(params, sessions::sessionsFor);
        SessionPayload payload = SidecarJson.params(params, WATCH.params());
        return new SidecarAnswer(SidecarJson.tree(current), () -> watchOnce(payload));
    }

    /** A watch that failed to open is forgotten, so the next {@code session/watch} of the project tries again. */
    private void watchOnce(SessionPayload payload) {
        if (watching.add(payload.root())) {
            try {
                sessions.subscribe(payload, view -> output.send(CHANGED.message(view)));
            } catch (RuntimeException failure) {
                watching.remove(payload.root());
                throw failure;
            }
        }
    }
}

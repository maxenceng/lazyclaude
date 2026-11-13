package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewSessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyOutputView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RepaintPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumePastPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionExitView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeView;
import dev.maxenceng.lazyclaude.session.published.HostStart;

import tools.jackson.databind.JsonNode;

/**
 * The {@code session/} and {@code pty/} methods: one per operation of {@link SessionQuery}, {@link HostedSessionCommand} and
 * {@link WatchedSessionCommand}, named
 * after it, and {@code session/configuration}, which answers the configuration read at start-up, not a second read of the file
 * that would raise its warnings again. The pty's bytes travel as {@code pty/input}, {@code pty/resize} and {@code pty/output} (ADR 0050). There is
 * no method to end a session: closing stdin ends every one.
 */
final class SessionMethods {

    private static final List<ServedMethod<HostedSessionCommand, ?, ?>> STARTS = List.of(
            ServedMethod.of("session/startUnlinked", NewHostedPayload.class, HostStart.class, HostedSessionCommand::startUnlinked),
            ServedMethod.of("session/startTicket", TicketHostedPayload.class, HostStart.class, HostedSessionCommand::startTicket),
            ServedMethod.of("session/resumePast", ResumePastPayload.class, HostStart.class, HostedSessionCommand::resumePast));
    private static final ServedMethod<SessionQuery, NewSessionPayload, NewSessionView> NEW_SESSION =
            ServedMethod.of("session/newSessionFor", NewSessionPayload.class, NewSessionView.class, SessionQuery::newSessionFor);
    private static final ServedMethod<SessionQuery, WorktreePayload, WorktreeView> WORKTREE =
            ServedMethod.of("session/worktreeOf", WorktreePayload.class, WorktreeView.class, SessionQuery::worktreeOf);
    private static final ServedMethod<ConfigurationView, NoParams, ConfigurationView> CONFIGURATION =
            ServedMethod.of("session/configuration", NoParams.class, ConfigurationView.class, (held, _) -> held);
    private static final ProtocolMethod<SessionPayload, SessionsView> WATCH =
            new ProtocolMethod<>("session/watch", SessionPayload.class, SessionsView.class);
    private static final List<ServedInput<HostedSessionCommand, ?>> INPUTS = List.of(
            ServedInput.of("pty/input", PtyInputPayload.class, HostedSessionCommand::write),
            ServedInput.of("pty/resize", PtyResizePayload.class, HostedSessionCommand::resizePty),
            ServedInput.of("session/repaint", RepaintPayload.class, HostedSessionCommand::repaint));
    private static final ServedInput<WatchedSessionCommand, WatchedPayload> WATCHING =
            ServedInput.of("session/watching", WatchedPayload.class, WatchedSessionCommand::watching);
    private static final ProtocolNotification<SessionsView> CHANGED = new ProtocolNotification<>("session/changed", SessionsView.class);
    private static final ProtocolNotification<PtyOutputView> OUTPUT = new ProtocolNotification<>("pty/output", PtyOutputView.class);
    private static final ProtocolNotification<SessionExitView> EXITED = new ProtocolNotification<>("session/exited", SessionExitView.class);

    private final SessionQuery sessions;
    private final HostedSessionCommand hosting;
    private final WatchedSessionCommand watched;
    private final ConfigurationView configuration;
    private final SidecarOutput output;
    private final Set<Path> watching = ConcurrentHashMap.newKeySet();

    SessionMethods(
            SessionQuery sessions,
            HostedSessionCommand hosting,
            WatchedSessionCommand watched,
            ConfigurationView configuration,
            SidecarOutput output) {
        this.sessions = sessions;
        this.hosting = hosting;
        this.watched = watched;
        this.configuration = configuration;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(
                Stream.concat(STARTS.stream().map(ServedMethod::method), Stream.of(NEW_SESSION.method(), WORKTREE.method(), CONFIGURATION.method(), WATCH))
                        .<ProtocolMethod<?, ?>>map(declared -> declared)
                        .toList(),
                Stream.concat(INPUTS.stream().map(ServedInput::input), Stream.of(WATCHING.input()))
                        .<ProtocolInput<?>>map(declared -> declared)
                        .toList(),
                List.of(CHANGED, OUTPUT, EXITED));
    }

    /** The methods, and the pushes of what a hosted {@code claude} writes and how it exits wired to the output. */
    Map<String, SidecarMethod> methods() {
        sessions.onOutput(view -> output.send(OUTPUT.message(view)));
        sessions.onExit(view -> output.send(EXITED.message(view)));
        return Stream.of(
                        STARTS.stream().map(start -> start.boundTo(hosting)),
                        INPUTS.stream().map(input -> input.boundTo(hosting)),
                        Stream.of(WATCHING.boundTo(watched)),
                        Stream.of(NEW_SESSION.boundTo(sessions)),
                        Stream.of(WORKTREE.boundTo(sessions)),
                        Stream.of(CONFIGURATION.boundTo(configuration)),
                        Stream.of(Map.<String, SidecarMethod>entry(WATCH.name(), this::watch)))
                .flatMap(served -> served)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** The watch opens once the current sessions are queued, so no {@code session/changed} reaches the frontend before the sessions it changes. */
    // deferred: a change between this read and the watch starting is pushed only at the next change — close it when a session started as the frontend connects is seen stale.
    private SidecarAnswer watch(JsonNode params) {
        SessionsView current = WATCH.answer(params, sessions::sessionsFor);
        SessionPayload payload = SidecarJson.params(params, WATCH.params());
        return new SidecarAnswer(SidecarJson.tree(current), () -> watchOnce(payload));
    }

    /** A watch that failed to open is forgotten before another {@code session/watch} of the project is let in, so that one tries again. */
    private synchronized void watchOnce(SessionPayload payload) {
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

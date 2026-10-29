package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostLaunch;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LaunchCommand;
import dev.maxenceng.lazyclaude.session.domain.NewClaude;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.domain.ResumeClaude;
import dev.maxenceng.lazyclaude.session.domain.TicketClaude;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Runs {@code claude} on a pty with pty4j and carries the bytes both ways, emulating nothing: what the child writes goes to
 * the listener as it was read, and what is written to it goes in unchanged. The child's environment is lazyclaude's less
 * everything that names another Claude Code session, kitty or tmux.
 */
public final class PtyHostRepository implements HostPort {

    private static final String SESSION_ID_FLAG = "--session-id";
    private static final String RESUME_FLAG = "--resume";
    private static final String NAME_FLAG = "-n";
    private static final String SLASH_COMMAND = "/ticket %s %s";

    private final List<String> claude;
    private final Map<String, String> environment;
    private final Map<SessionId, HostedPty> running = new ConcurrentHashMap<>();

    /** {@code claude} is the command and arguments that start Claude Code; {@code environment} is what the child inherits, less what it must not. */
    public PtyHostRepository(List<String> claude, Map<String, String> environment) {
        this.claude = claude;
        this.environment = environment;
    }

    @Override
    public ProcessId spawn(HostLaunch launch, Consumer<PtyBytes> onOutput, Consumer<ExitCode> onExit) {
        HostedPty pty = HostedPty.spawn(
                argv(launch.command()), ScrubbedEnvironment.of(environment), launch.directory().path(), launch.size());
        SessionId id = launch.command().session();
        running.put(id, pty);
        pty.listen(
                onOutput,
                code -> {
                    running.remove(id, pty);
                    onExit.accept(code);
                });
        return pty.pid();
    }

    @Override
    public boolean exists(WorkingDirectory directory) {
        return Files.isDirectory(directory.path());
    }

    @Override
    public void write(SessionId id, PtyBytes bytes) {
        pty(id).ifPresent(pty -> pty.write(bytes));
    }

    @Override
    public void resize(SessionId id, PaneSize size) {
        pty(id).ifPresent(pty -> pty.resize(size));
    }

    @Override
    public void end(SessionId id) {
        pty(id).ifPresent(HostedPty::end);
    }

    private Optional<HostedPty> pty(SessionId id) {
        return Optional.ofNullable(running.get(id));
    }

    private List<String> argv(LaunchCommand command) {
        return switch (command) {
            case NewClaude begun -> Stream.concat(claude.stream(), Stream.of(SESSION_ID_FLAG, begun.session().value()))
                    .toList();
            case ResumeClaude resume -> Stream.concat(claude.stream(), Stream.of(RESUME_FLAG, resume.session().value()))
                    .toList();
            case TicketClaude run -> Stream.concat(
                            claude.stream(),
                            Stream.of(
                                    SESSION_ID_FLAG,
                                    run.session().value(),
                                    NAME_FLAG,
                                    run.sessionName().value(),
                                    SLASH_COMMAND.formatted(run.verb().label(), run.ticket().value())))
                    .toList();
        };
    }
}

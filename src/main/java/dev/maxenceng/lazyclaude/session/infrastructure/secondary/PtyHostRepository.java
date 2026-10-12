package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostLaunch;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LaunchCommand;
import dev.maxenceng.lazyclaude.session.domain.NewClaude;
import dev.maxenceng.lazyclaude.session.domain.ResumeClaude;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Runs {@code claude} on a pty with pty4j, reads its output through jediterm's emulator, and turns the shell's keys, pastes
 * and mouse events into the bytes a terminal sends. It is where the screen library's types stop: the emulator's cell grid
 * leaves as a {@link TerminalScreen}. The child's environment is lazyclaude's less everything that names another Claude Code
 * session, kitty or tmux.
 */
public final class PtyHostRepository implements HostPort {

    private static final String RESUME_FLAG = "--resume";

    private final List<String> claude;
    private final Map<String, String> environment;
    private final Map<HostedSessionId, HostedPty> running = new ConcurrentHashMap<>();
    private final List<Consumer<HostedSessionId>> screenListeners = new CopyOnWriteArrayList<>();

    /** {@code claude} is the command and arguments that start Claude Code; {@code environment} is what the child inherits, less what it must not. */
    public PtyHostRepository(List<String> claude, Map<String, String> environment) {
        this.claude = List.copyOf(claude);
        this.environment = environment;
    }

    @Override
    public ProcessId spawn(HostLaunch launch, Consumer<ExitCode> onExit) {
        HostedPty pty = HostedPty.spawn(
                argv(launch.command()), ScrubbedEnvironment.of(environment), launch.directory().path(), launch.size());
        HostedSessionId id = launch.id();
        running.put(id, pty);
        pty.listen(
                () -> screenListeners.forEach(listener -> listener.accept(id)),
                code -> {
                    running.remove(id);
                    onExit.accept(code);
                });
        return pty.pid();
    }

    @Override
    public boolean exists(WorkingDirectory directory) {
        return Files.isDirectory(directory.path());
    }

    @Override
    public void send(HostedSessionId id, TerminalInput input) {
        pty(id).ifPresent(pty -> pty.send(input));
    }

    @Override
    public void resize(HostedSessionId id, PaneSize size) {
        pty(id).ifPresent(pty -> pty.resize(size));
    }

    @Override
    public TerminalScreen screenOf(HostedSessionId id) {
        return pty(id).map(HostedPty::screen).orElseGet(TerminalScreen::blank);
    }

    @Override
    public void end(HostedSessionId id) {
        pty(id).ifPresent(HostedPty::end);
    }

    @Override
    public void onScreenChange(Consumer<HostedSessionId> listener) {
        screenListeners.add(listener);
    }

    private Optional<HostedPty> pty(HostedSessionId id) {
        return Optional.ofNullable(running.get(id));
    }

    private List<String> argv(LaunchCommand command) {
        return switch (command) {
            case NewClaude plain -> claude;
            case ResumeClaude resume -> Stream.concat(claude.stream(), Stream.of(RESUME_FLAG, resume.session().value()))
                    .toList();
        };
    }
}

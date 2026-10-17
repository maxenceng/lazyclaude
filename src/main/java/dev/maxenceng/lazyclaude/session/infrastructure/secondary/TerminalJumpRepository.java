package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.InKitty;
import dev.maxenceng.lazyclaude.session.domain.InTmux;
import dev.maxenceng.lazyclaude.session.domain.RemoteControlOffException;
import dev.maxenceng.lazyclaude.session.domain.TerminalJumpFailedException;
import dev.maxenceng.lazyclaude.session.domain.TerminalJumpPort;

/**
 * Brings a window forward with {@code kitten @}, over the socket of the kitty that owns the window, or with
 * {@code tmux switch-client}. kitty answers a command it will not take with a non-zero exit: {@code Remote control is ...} when
 * it listens but does not allow it, and a connection that fails when it listens on no socket, which is what {@code allow_remote_control no} comes to;
 * those two are told apart from any other failure, which is not about remote control.
 */
public final class TerminalJumpRepository implements TerminalJumpPort {

    private static final List<String> REMOTE_CONTROL_REFUSALS = List.of("Remote control is", "Failed to connect");
    private static final String REMOTE = "@";
    private static final String TO = "--to";
    private static final String SOCKET = "unix:@kitty-%d";
    private static final String FOCUS_WINDOW = "focus-window";
    private static final String MATCH = "--match";
    private static final String WINDOW_ID = "id:%d";
    private static final String SWITCH_CLIENT = "switch-client";
    private static final String TARGET = "-t";
    private static final int SUCCESS = 0;
    private static final String EXITED = "%s exited with %d: %s";

    private final List<String> kitten;
    private final List<String> tmux;
    private final Duration patience;

    public TerminalJumpRepository(List<String> kitten, List<String> tmux, Duration patience) {
        this.kitten = kitten;
        this.tmux = tmux;
        this.patience = patience;
    }

    @Override
    public void jump(InKitty window) {
        TerminalCommand focus = command(
                kitten,
                REMOTE,
                TO,
                SOCKET.formatted(window.kitty().value()),
                FOCUS_WINDOW,
                MATCH,
                WINDOW_ID.formatted(window.window().value()));
        CommandOutput result = focus.run();
        if (result.exit() == SUCCESS) {
            return;
        }
        if (REMOTE_CONTROL_REFUSALS.stream().anyMatch(result.output()::contains)) {
            throw new RemoteControlOffException(result.output().strip());
        }
        throw failed(focus, result);
    }

    @Override
    public void jump(InTmux pane) {
        TerminalCommand switching = command(tmux, SWITCH_CLIENT, TARGET, pane.pane().value());
        CommandOutput result = switching.run();
        if (result.exit() != SUCCESS) {
            throw failed(switching, result);
        }
    }

    private TerminalCommand command(List<String> program, String... arguments) {
        return new TerminalCommand(Stream.concat(program.stream(), Stream.of(arguments)).toList(), patience);
    }

    private static TerminalJumpFailedException failed(TerminalCommand command, CommandOutput result) {
        return new TerminalJumpFailedException(EXITED.formatted(command.program(), result.exit(), result.output().strip()));
    }
}

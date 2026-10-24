package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Brings an outside session's window forward, where it lives: a kitty window, or a tmux pane when lazyclaude runs in that
 * tmux server. A jump that cannot be made is a warning and nothing else; a session lazyclaude hosts is never jumped to.
 * When kitty refuses because remote control is off and lazyclaude itself runs in kitty, it answers what {@code kitty.conf}
 * needs, to be offered or shown, or warns that kitty only needs a restart. Outside kitty it never does, and a refused
 * offer is not remembered: the next refusal offers again.
 */
public final class JumpManager {

    private static final String CANNOT_JUMP = "cannot jump: %s";
    private static final String NEITHER = "the session lives in neither kitty nor tmux.";
    private static final String OTHER_TMUX_SERVER = "the session is in a tmux server lazyclaude is not attached to.";
    private static final String NOT_IN_KITTY =
            "kitty remote control is off, and lazyclaude does not run in kitty to offer turning it on.";

    private final TerminalJumpPort jumps;
    private final ProcessEnvironmentPort environment;
    private final KittyConfigPort config;
    private final WarningPort warnings;

    public JumpManager(
            TerminalJumpPort jumps, ProcessEnvironmentPort environment, KittyConfigPort config, WarningPort warnings) {
        this.jumps = jumps;
        this.environment = environment;
        this.config = config;
        this.warnings = warnings;
    }

    /** What {@code kitty.conf} needs for the jump to work next time, when kitty refused it for remote control and lazyclaude runs in kitty; else none. */
    public Optional<RemoteControlConfig> jump(LiveSession session) {
        if (!session.hosting().isJumpable()) {
            return Optional.empty();
        }
        ProcessEnvironment own = environment.ownEnvironment();
        return switch (session.residence()) {
            case InKitty window -> inKitty(window, own);
            case InTmux pane -> inTmux(pane, own);
            case Nowhere _ -> unjumpable(NEITHER);
        };
    }

    private Optional<RemoteControlConfig> inKitty(InKitty window, ProcessEnvironment own) {
        try {
            jumps.jump(window);
            return Optional.empty();
        } catch (RemoteControlOffException off) {
            return own.kitty().isPresent() ? remoteControlConfig() : unjumpable(NOT_IN_KITTY);
        } catch (TerminalJumpFailedException failed) {
            return unjumpable(failed.getMessage());
        }
    }

    // deferred: a session in a tmux server lazyclaude is not attached to cannot be jumped to — close it when someone needs that jump, through the attached client's kitty window.
    private Optional<RemoteControlConfig> inTmux(InTmux pane, ProcessEnvironment own) {
        if (!(own.residence() instanceof InTmux ours) || !ours.servesPaneOf(pane)) {
            return unjumpable(OTHER_TMUX_SERVER);
        }
        try {
            jumps.jump(pane);
            return Optional.empty();
        } catch (TerminalJumpFailedException failed) {
            return unjumpable(failed.getMessage());
        }
    }

    private Optional<RemoteControlConfig> remoteControlConfig() {
        try {
            KittyConfigFile file = config.locate();
            RemoteControlConfig needed = config.settings(file, KittySettings.remoteControlKeys()).remoteControl(file);
            return switch (needed) {
                case ConfiguredRemoteControl configured -> unjumpable(configured.needsRestart());
                case AddRemoteControl adding -> Optional.of(adding);
                case ConflictingRemoteControl conflicting -> Optional.of(conflicting);
            };
        } catch (KittyConfigUnreadableException unreadable) {
            return unjumpable(unreadable.getMessage());
        }
    }

    private Optional<RemoteControlConfig> unjumpable(String reason) {
        warnings.raise(new Warning(CANNOT_JUMP.formatted(reason)));
        return Optional.empty();
    }
}

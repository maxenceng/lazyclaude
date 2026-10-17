package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;

/** What the shell asks of {@code session} to act on an outside session's terminal: bring its window forward, and turn kitty's remote control on. Both run processes or touch files, so not on the render thread. */
public final class OutsideSessionCommand {

    private final SessionApplicationService sessions;

    public OutsideSessionCommand(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /**
     * Brings the session's kitty window or tmux pane forward. A jump that cannot be made is already a warning; when kitty refuses
     * because remote control is off and lazyclaude runs in kitty, what {@code kitty.conf} needs is answered, to be shown.
     */
    public Optional<RemoteControlConfigView> jump(JumpPayload payload) {
        return sessions.jump(payload.sessions().toDomain(), payload.sessions().tickets(), payload.session())
                .flatMap(RemoteControlConfigView::from);
    }

    /** Appends the lines to {@code kitty.conf}, as the user said yes to the popup; the outcome is a warning. */
    public void addRemoteControl() {
        sessions.addKittyRemoteControl();
    }
}

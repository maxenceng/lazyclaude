package dev.maxenceng.lazyclaude.session.domain;

/** Brings the window a live session runs in to the front. */
public interface TerminalJumpPort {

    /**
     * Focuses the kitty window, over the socket of the kitty that owns it.
     *
     * @throws RemoteControlOffException when kitty refuses because remote control is not allowed, or nothing listens
     * @throws TerminalJumpFailedException when the jump fails for any other reason, with the reason
     */
    void jump(InKitty window);

    /**
     * Switches the tmux client lazyclaude runs in to the pane.
     *
     * @throws TerminalJumpFailedException when tmux cannot be run or refuses, with the reason
     */
    void jump(InTmux pane);
}

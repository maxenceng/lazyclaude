package dev.maxenceng.lazyclaude.session.domain;

/** Both options are already set as wanted, so the running kitty only needs a restart. */
public record ConfiguredRemoteControl() implements RemoteControlConfig {

    private static final String NEEDS_RESTART = "kitty.conf already allows remote control, so kitty needs a restart.";

    /** What the person is told, wherever it is found that kitty only needs a restart. */
    public String needsRestart() {
        return NEEDS_RESTART;
    }
}

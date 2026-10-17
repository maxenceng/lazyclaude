package dev.maxenceng.lazyclaude.session.domain;

/** Both options are already set as wanted, so the running kitty only needs a restart. */
public record ConfiguredRemoteControl() implements RemoteControlConfig {}

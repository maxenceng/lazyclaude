package dev.maxenceng.lazyclaude.session.domain;

/** What kitty's configuration needs for remote control over a socket: lines to add, a line that conflicts, or nothing. */
public sealed interface RemoteControlConfig permits AddRemoteControl, ConflictingRemoteControl, ConfiguredRemoteControl {}

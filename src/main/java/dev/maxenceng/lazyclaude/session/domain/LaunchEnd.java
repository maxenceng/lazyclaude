package dev.maxenceng.lazyclaude.session.domain;

/** How a launch-log entry's session ended: when its {@code claude} exited by itself, else not at all (ADR 0048). */
public sealed interface LaunchEnd permits EndedAt, NotEnded {}

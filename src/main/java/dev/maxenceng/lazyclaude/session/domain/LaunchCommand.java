package dev.maxenceng.lazyclaude.session.domain;

/** What the hosted {@code claude} is asked to do: begin a session, or pick one up again. */
public sealed interface LaunchCommand permits NewClaude, ResumeClaude {}

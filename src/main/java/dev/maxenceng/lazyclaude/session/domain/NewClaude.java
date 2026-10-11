package dev.maxenceng.lazyclaude.session.domain;

/** A plain {@code claude}, which begins a session of its own. */
public record NewClaude() implements LaunchCommand {}

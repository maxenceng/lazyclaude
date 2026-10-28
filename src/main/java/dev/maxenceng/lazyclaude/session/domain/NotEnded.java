package dev.maxenceng.lazyclaude.session.domain;

/** No end was recorded: the session is still running, or lazyclaude's quit, a crash or a kill cut it off. */
public record NotEnded() implements LaunchEnd {}

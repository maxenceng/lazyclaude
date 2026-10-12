package dev.maxenceng.lazyclaude.session.published;

/** The outcome of starting or resuming a hosted session. */
public sealed interface HostStart permits StartedHost, NotStartedHost {}

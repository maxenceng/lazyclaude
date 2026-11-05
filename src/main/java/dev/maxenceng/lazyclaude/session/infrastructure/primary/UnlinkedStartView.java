package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** The ticket allows no verb: a session started from it is an unlinked one, started as {@link HostedSessionCommand#startUnlinked} does. */
public record UnlinkedStartView() implements NewSessionView {}

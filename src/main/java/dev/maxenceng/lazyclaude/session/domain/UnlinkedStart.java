package dev.maxenceng.lazyclaude.session.domain;

/** The ticket allows no verb, so a session started from it is an unlinked one in the project root. */
public record UnlinkedStart() implements NewSession {}

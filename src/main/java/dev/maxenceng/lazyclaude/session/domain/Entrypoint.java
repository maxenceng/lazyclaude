package dev.maxenceng.lazyclaude.session.domain;

/** How a session was started, as its record says; {@code claude agents --json} does not say. */
public sealed interface Entrypoint permits SdkCliEntrypoint, OtherEntrypoint, NoEntrypoint {}

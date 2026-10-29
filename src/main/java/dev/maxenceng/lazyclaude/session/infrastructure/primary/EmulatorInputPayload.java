package dev.maxenceng.lazyclaude.session.infrastructure.primary;

/** What the shell hands a hosted session: a key, a paste, or a mouse event, in lazyclaude's types and not the screen library's. */
public sealed interface EmulatorInputPayload permits EmulatorKeyPayload, EmulatorPastePayload, EmulatorMousePayload {}

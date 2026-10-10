package dev.maxenceng.lazyclaude.session.published;

/** What a session is doing, in lazyclaude's words: working, waiting for your next prompt, blocked on you, or not known. */
public sealed interface SessionState permits Busy, YourTurn, WaitingForPermission, WaitingForInput, Unknown {}

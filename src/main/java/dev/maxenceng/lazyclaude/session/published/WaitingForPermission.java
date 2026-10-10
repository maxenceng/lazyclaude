package dev.maxenceng.lazyclaude.session.published;

/** The session is blocked on a permission prompt. */
public record WaitingForPermission() implements SessionState {}

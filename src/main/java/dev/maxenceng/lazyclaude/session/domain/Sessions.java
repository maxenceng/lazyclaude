package dev.maxenceng.lazyclaude.session.domain;

/** What a project's sessions are as the Sessions panel shows them: the live and past ones, or the reason none could be read. */
public sealed interface Sessions permits AvailableSessions, UnavailableSessions {}

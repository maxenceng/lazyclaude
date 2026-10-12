package dev.maxenceng.lazyclaude.session.domain;

/** What a project's sessions are as the Sessions panel shows them: the live and past ones, or the reason none could be read. */
public sealed interface Sessions permits AvailableSessions, UnavailableSessions {

    /** These sessions with {@code open} laid over them: the live ones lazyclaude spawned marked hosted, and a tab for each. */
    Sessions hosting(HostedSessions open);
}

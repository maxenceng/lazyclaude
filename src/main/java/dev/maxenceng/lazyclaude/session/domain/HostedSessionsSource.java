package dev.maxenceng.lazyclaude.session.domain;

/** Where the hosted sessions that are open are read from, each time the live sessions are. */
@FunctionalInterface
public interface HostedSessionsSource {

    HostedSessions open();
}

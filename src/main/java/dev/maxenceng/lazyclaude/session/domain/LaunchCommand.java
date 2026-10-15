package dev.maxenceng.lazyclaude.session.domain;

/** What the hosted {@code claude} is asked to do: begin a session, pick one up again, or run {@code /ticket} on a ticket. */
public sealed interface LaunchCommand permits NewClaude, ResumeClaude, TicketClaude {}

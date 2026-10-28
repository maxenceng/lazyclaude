package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkVerb;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.StrongLink;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * A {@code claude} process lazyclaude owns, its tab named by the session id it was launched with: starting while no registry record
 * carries its pid, then live as the session that record describes, which {@code /clear} replaces, until its process exits and it is
 * dropped.
 */
public record HostedSession(ProcessId pid, WorkingDirectory directory, LaunchCommand command, StartedAt spawnedAt) {

    private static final String PLAIN_NAME = "claude";

    public HostedSession {
        Assert.notNull("pid", pid);
        Assert.notNull("directory", directory);
        Assert.notNull("command", command);
        Assert.notNull("spawned at", spawnedAt);
    }

    /** The id it was launched with, or the one it resumed: what its tab and its pty are known by, whatever session it runs now. */
    public SessionId id() {
        return command.session();
    }

    /** The ticket this session was started for, none for a plain or resumed one. */
    public Optional<LinkedTicketId> ticket() {
        return command instanceof TicketClaude(_, LinkedTicketId ticket, _) ? Optional.of(ticket) : Optional.empty();
    }

    /**
     * Whether {@code record} was written by this process, and not left by a {@code claude} that held its pid before and died: a record
     * started earlier than this spawn is the other one's.
     */
    public boolean reportedBy(ReportedSession record) {
        return !record.startedAt().isBefore(spawnedAt);
    }

    /** Whether its {@code claude} runs in {@code worktree} or below it. */
    public boolean runsIn(WorkingDirectory worktree) {
        return worktree.contains(directory);
    }

    /** What it is called: the name {@code claude} was given for its ticket and verb, else plain {@code claude}. */
    public SessionName name() {
        return command instanceof TicketClaude run ? run.sessionName() : new SessionName(PLAIN_NAME);
    }

    /** The ticket this session was started to close out with {@code /ticket done}, none for a session that ran any other verb. */
    public Optional<LinkedTicketId> doneTicket() {
        return command instanceof TicketClaude(_, LinkedTicketId ticket, Verb verb) && verb == Verb.DONE ? Optional.of(ticket) : Optional.empty();
    }

    /** The link to its ticket that starting it gives it before any transcript does. */
    public Optional<StrongLink> startingLink() {
        return command instanceof TicketClaude(_, LinkedTicketId ticket, Verb verb)
                ? Optional.of(StrongLink.builder()
                        .ticket(ticket)
                        .verb(new LinkVerb(verb.label()))
                        .build())
                : Optional.empty();
    }

    /** Whether this session was started to run {@code verb} on {@code ticket}. */
    public boolean runs(LinkedTicketId ticket, Verb verb) {
        return command instanceof TicketClaude(_, LinkedTicketId started, Verb ran) && started.equals(ticket) && ran == verb;
    }

    public static HostedSessionBuilder builder() {
        return new HostedSessionBuilder();
    }

    public static final class HostedSessionBuilder {
        private ProcessId pid;
        private WorkingDirectory directory;
        private LaunchCommand command;
        private StartedAt spawnedAt;

        private HostedSessionBuilder() {}

        public HostedSessionBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public HostedSessionBuilder directory(WorkingDirectory directory) {
            this.directory = directory;
            return this;
        }

        public HostedSessionBuilder command(LaunchCommand command) {
            this.command = command;
            return this;
        }

        public HostedSessionBuilder spawnedAt(StartedAt spawnedAt) {
            this.spawnedAt = spawnedAt;
            return this;
        }

        public HostedSession build() {
            return new HostedSession(pid, directory, command, spawnedAt);
        }
    }
}

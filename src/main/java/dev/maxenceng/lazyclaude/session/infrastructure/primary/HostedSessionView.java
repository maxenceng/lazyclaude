package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.HostedPhase;
import dev.maxenceng.lazyclaude.session.domain.OpenHostedSession;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** One hosted session as the dock's tab reads it: starting, or the live session its registry record says it is, unknown when the record cannot be read, and the ticket it was started for. */
public record HostedSessionView(
        SessionId id,
        ProcessId pid,
        Optional<LiveSessionView> live,
        boolean busyOrWaiting,
        Optional<LinkedTicketId> ticket) {

    static HostedSessionView from(AvailableSessions available, OpenHostedSession open) {
        return builder()
                .id(open.session().id())
                .pid(open.session().pid())
                .live(liveOf(available, open.phase()))
                .busyOrWaiting(open.phase().isBusyOrWaiting())
                .ticket(open.session().ticket())
                .build();
    }

    private static Optional<LiveSessionView> liveOf(AvailableSessions available, HostedPhase phase) {
        return phase.live().map(live -> LiveSessionView.from(available, live));
    }

    public static HostedSessionViewBuilder builder() {
        return new HostedSessionViewBuilder();
    }

    public static final class HostedSessionViewBuilder {
        private SessionId id;
        private ProcessId pid;
        private Optional<LiveSessionView> live = Optional.empty();
        private boolean busyOrWaiting;
        private Optional<LinkedTicketId> ticket = Optional.empty();

        private HostedSessionViewBuilder() {}

        public HostedSessionViewBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public HostedSessionViewBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public HostedSessionViewBuilder live(Optional<LiveSessionView> live) {
            this.live = live;
            return this;
        }

        public HostedSessionViewBuilder busyOrWaiting(boolean busyOrWaiting) {
            this.busyOrWaiting = busyOrWaiting;
            return this;
        }

        public HostedSessionViewBuilder ticket(Optional<LinkedTicketId> ticket) {
            this.ticket = ticket;
            return this;
        }

        public HostedSessionView build() {
            return new HostedSessionView(id, pid, live, busyOrWaiting, ticket);
        }
    }
}

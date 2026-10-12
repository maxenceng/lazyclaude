package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.HostedPhase;
import dev.maxenceng.lazyclaude.session.domain.LivePhase;
import dev.maxenceng.lazyclaude.session.domain.OpenHostedSession;
import dev.maxenceng.lazyclaude.session.domain.StartingPhase;
import dev.maxenceng.lazyclaude.session.domain.UnreadPhase;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** One hosted session as the dock's tab reads it: starting, or the live session the registry says it is. */
public record HostedSessionView(
        HostedSessionId id, ProcessId pid, Optional<LiveSessionView> live, boolean busyOrWaiting) {

    static HostedSessionView from(AvailableSessions available, OpenHostedSession open) {
        return builder()
                .id(open.session().id())
                .pid(open.session().pid())
                .live(liveOf(available, open.phase()))
                .busyOrWaiting(open.phase().isBusyOrWaiting())
                .build();
    }

    private static Optional<LiveSessionView> liveOf(AvailableSessions available, HostedPhase phase) {
        return switch (phase) {
            case StartingPhase starting -> Optional.empty();
            case LivePhase live -> Optional.of(LiveSessionView.from(available, live.session()));
            case UnreadPhase unread -> Optional.empty();
        };
    }

    public static HostedSessionViewBuilder builder() {
        return new HostedSessionViewBuilder();
    }

    public static final class HostedSessionViewBuilder {
        private HostedSessionId id;
        private ProcessId pid;
        private Optional<LiveSessionView> live = Optional.empty();
        private boolean busyOrWaiting;

        private HostedSessionViewBuilder() {}

        public HostedSessionViewBuilder id(HostedSessionId id) {
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

        public HostedSessionView build() {
            return new HostedSessionView(id, pid, live, busyOrWaiting);
        }
    }
}

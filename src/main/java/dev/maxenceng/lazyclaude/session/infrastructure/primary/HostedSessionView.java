package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.HostedPhase;
import dev.maxenceng.lazyclaude.session.domain.HostedTab;
import dev.maxenceng.lazyclaude.session.domain.RegisteredPhase;
import dev.maxenceng.lazyclaude.session.domain.StartingPhase;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** One hosted session as the dock's tab reads it: starting, or the live session the registry says it is. */
public record HostedSessionView(
        HostedSessionId id, ProcessId pid, Optional<LiveSessionView> registered, boolean busyOrWaiting) {

    static HostedSessionView from(AvailableSessions available, HostedTab tab) {
        return builder()
                .id(tab.session().id())
                .pid(tab.session().pid())
                .registered(registeredOf(available, tab.phase()))
                .busyOrWaiting(tab.phase().isBusyOrWaiting())
                .build();
    }

    private static Optional<LiveSessionView> registeredOf(AvailableSessions available, HostedPhase phase) {
        return switch (phase) {
            case StartingPhase starting -> Optional.empty();
            case RegisteredPhase registered -> Optional.of(LiveSessionView.from(available, registered.session()));
        };
    }

    public static HostedSessionViewBuilder builder() {
        return new HostedSessionViewBuilder();
    }

    public static final class HostedSessionViewBuilder {
        private HostedSessionId id;
        private ProcessId pid;
        private Optional<LiveSessionView> registered = Optional.empty();
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

        public HostedSessionViewBuilder registered(Optional<LiveSessionView> registered) {
            this.registered = registered;
            return this;
        }

        public HostedSessionViewBuilder busyOrWaiting(boolean busyOrWaiting) {
            this.busyOrWaiting = busyOrWaiting;
            return this;
        }

        public HostedSessionView build() {
            return new HostedSessionView(id, pid, registered, busyOrWaiting);
        }
    }
}

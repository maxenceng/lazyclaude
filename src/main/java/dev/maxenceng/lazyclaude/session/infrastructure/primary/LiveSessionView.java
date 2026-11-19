package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.LiveSession;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/**
 * One hosted live session as the shell reads it: what its row and the main view show, what it asks of you, and the tickets it is
 * linked to. The marker is always the state's, so the builder takes none and the constructor refuses any other.
 */
public record LiveSessionView(
        SessionId id,
        ProcessId pid,
        SessionName name,
        Path directory,
        LocalDateTime startedAt,
        ClaudeVersion version,
        SessionState state,
        Optional<MarkerView> marker,
        List<TicketLink> links) {

    public LiveSessionView {
        Assert.notNull("state", state);
        Assert.notNull("marker", marker);
        if (!marker.equals(markerOf(state))) {
            throw new MarkerBesideStateException(state, marker);
        }
    }

    static LiveSessionView from(AvailableSessions available, LiveSession session) {
        return builder()
                .id(session.id())
                .pid(session.pid())
                .name(session.name())
                .directory(session.directory().path())
                .startedAt(available.startedLocally(session))
                .version(session.version())
                .state(session.state())
                .links(session.links())
                .build();
    }

    private static Optional<MarkerView> markerOf(SessionState state) {
        return state.marker().map(MarkerView::of);
    }

    /** How the session is linked to {@code ticket}, if it is. */
    public Optional<TicketLink> linkTo(LinkedTicketId ticket) {
        return TicketLink.to(links, ticket);
    }

    public static LiveSessionViewBuilder builder() {
        return new LiveSessionViewBuilder();
    }

    public static final class LiveSessionViewBuilder {
        private SessionId id;
        private ProcessId pid;
        private SessionName name;
        private Path directory;
        private LocalDateTime startedAt;
        private ClaudeVersion version;
        private SessionState state;
        private List<TicketLink> links;

        private LiveSessionViewBuilder() {}

        public LiveSessionViewBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public LiveSessionViewBuilder pid(ProcessId pid) {
            this.pid = pid;
            return this;
        }

        public LiveSessionViewBuilder name(SessionName name) {
            this.name = name;
            return this;
        }

        public LiveSessionViewBuilder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public LiveSessionViewBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public LiveSessionViewBuilder version(ClaudeVersion version) {
            this.version = version;
            return this;
        }

        public LiveSessionViewBuilder state(SessionState state) {
            this.state = state;
            return this;
        }

        public LiveSessionViewBuilder links(List<TicketLink> links) {
            this.links = links;
            return this;
        }

        public LiveSessionView build() {
            return new LiveSessionView(id, pid, name, directory, startedAt, version, state, markerOf(state), links);
        }
    }
}

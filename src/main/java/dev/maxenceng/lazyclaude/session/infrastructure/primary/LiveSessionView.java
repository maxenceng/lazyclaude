package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.Hosting;
import dev.maxenceng.lazyclaude.session.domain.InKitty;
import dev.maxenceng.lazyclaude.session.domain.InTmux;
import dev.maxenceng.lazyclaude.session.domain.LiveSession;
import dev.maxenceng.lazyclaude.session.domain.Nowhere;
import dev.maxenceng.lazyclaude.session.domain.Residence;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.ResidenceName;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/** One live session as the shell reads it: what its row and the main view show, and the tickets it is linked to. */
public record LiveSessionView(
        SessionId id,
        ProcessId pid,
        SessionName name,
        Path directory,
        LocalDateTime startedAt,
        SessionAge age,
        ClaudeVersion version,
        SessionState state,
        List<TicketLink> links,
        Hosting hosting,
        Optional<ResidenceName> residenceName) {

    private static final ResidenceName KITTY = new ResidenceName("kitty");
    private static final ResidenceName TMUX = new ResidenceName("tmux");


    static LiveSessionView from(AvailableSessions available, LiveSession session) {
        return builder()
                .id(session.id())
                .pid(session.pid())
                .name(session.name())
                .directory(session.directory().path())
                .startedAt(available.startedLocally(session))
                .age(available.ageOf(session))
                .version(session.version())
                .state(session.state())
                .links(session.links())
                .hosting(session.hosting())
                .residenceName(nameOf(session.residence()))
                .build();
    }

    /** What the session asks of you, if anything. */
    public Optional<Marker> marker() {
        return state.marker();
    }

    /** How the session is linked to {@code ticket}, if it is. */
    public Optional<TicketLink> linkTo(LinkedTicketId ticket) {
        return TicketLink.to(links, ticket);
    }

    /** Whether lazyclaude spawned the session, so that it runs in a dock tab. */
    public boolean isHosted() {
        return hosting == Hosting.HOSTED;
    }

    /** Whether {@code o} brings the session's window forward. */
    public boolean isJumpable() {
        return hosting.isJumpable();
    }

    /** What the session's row says of where it lives, none when it lives in neither kitty nor tmux. */
    private static Optional<ResidenceName> nameOf(Residence residence) {
        return switch (residence) {
            case InKitty _ -> Optional.of(KITTY);
            case InTmux _ -> Optional.of(TMUX);
            case Nowhere _ -> Optional.empty();
        };
    }


    /** Whether the session is linked to no ticket. */
    public boolean isUnlinked() {
        return links.isEmpty();
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
        private SessionAge age;
        private ClaudeVersion version;
        private SessionState state;
        private List<TicketLink> links;
        private Hosting hosting = Hosting.OUTSIDE;
        private Optional<ResidenceName> residenceName = Optional.empty();

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

        public LiveSessionViewBuilder age(SessionAge age) {
            this.age = age;
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

        public LiveSessionViewBuilder hosting(Hosting hosting) {
            this.hosting = hosting;
            return this;
        }

        public LiveSessionViewBuilder residenceName(Optional<ResidenceName> residenceName) {
            this.residenceName = residenceName;
            return this;
        }

        public LiveSessionView build() {
            return new LiveSessionView(id, pid, name, directory, startedAt, age, version, state, links, hosting, residenceName);
        }
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.PastSession;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/** One past session as the shell reads it: what its row and the main view show, and the tickets it is linked to. */
public record PastSessionView(
        SessionId id,
        SessionTitle title,
        Path directory,
        LocalDateTime lastActivity,
        SessionAge age,
        LocalDateTime startedAt,
        List<TicketLink> links) {

    static PastSessionView from(AvailableSessions available, PastSession session) {
        return builder()
                .id(session.id())
                .title(session.title())
                .directory(session.directory().path())
                .lastActivity(available.lastActiveLocally(session))
                .age(available.ageOf(session))
                .startedAt(available.startedLocally(session))
                .links(session.links())
                .build();
    }

    /** How the session is linked to {@code ticket}, if it is. */
    public Optional<TicketLink> linkTo(String ticket) {
        return TicketLink.to(links, ticket);
    }

    /** Whether the session is linked to no ticket. */
    public boolean isUnlinked() {
        return links.isEmpty();
    }

    public static PastSessionViewBuilder builder() {
        return new PastSessionViewBuilder();
    }

    public static final class PastSessionViewBuilder {
        private SessionId id;
        private SessionTitle title;
        private Path directory;
        private LocalDateTime lastActivity;
        private SessionAge age;
        private LocalDateTime startedAt;
        private List<TicketLink> links;

        private PastSessionViewBuilder() {}

        public PastSessionViewBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public PastSessionViewBuilder title(SessionTitle title) {
            this.title = title;
            return this;
        }

        public PastSessionViewBuilder directory(Path directory) {
            this.directory = directory;
            return this;
        }

        public PastSessionViewBuilder lastActivity(LocalDateTime lastActivity) {
            this.lastActivity = lastActivity;
            return this;
        }

        public PastSessionViewBuilder age(SessionAge age) {
            this.age = age;
            return this;
        }

        public PastSessionViewBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public PastSessionViewBuilder links(List<TicketLink> links) {
            this.links = links;
            return this;
        }

        public PastSessionView build() {
            return new PastSessionView(id, title, directory, lastActivity, age, startedAt, links);
        }
    }
}

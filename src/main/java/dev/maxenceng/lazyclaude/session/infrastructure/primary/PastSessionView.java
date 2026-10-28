package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.PastSession;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.OpeningPrompt;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.TranscriptReading;

/** One past session as the shell reads it: what its row and the main view show, the tickets it is linked to, and whether it was cut off rather than ended by its {@code claude}. */
public record PastSessionView(
        SessionId id,
        SessionTitle title,
        OpeningPrompt opening,
        TranscriptReading reading,
        Path directory,
        LocalDateTime lastActivity,
        SessionAge age,
        LocalDateTime startedAt,
        List<TicketLink> links,
        boolean interrupted) {

    static PastSessionView from(AvailableSessions available, PastSession session) {
        return builder()
                .id(session.id())
                .title(session.title())
                .opening(session.opening())
                .reading(session.reading())
                .directory(session.directory().path())
                .lastActivity(available.lastActiveLocally(session))
                .age(available.ageOf(session))
                .startedAt(available.startedLocally(session))
                .links(session.links())
                .interrupted(session.interrupted())
                .build();
    }

    /** How the session is linked to {@code ticket}, if it is. */
    public Optional<TicketLink> linkTo(LinkedTicketId ticket) {
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
        private OpeningPrompt opening;
        private TranscriptReading reading;
        private Path directory;
        private LocalDateTime lastActivity;
        private SessionAge age;
        private LocalDateTime startedAt;
        private List<TicketLink> links;
        private boolean interrupted;

        private PastSessionViewBuilder() {}

        public PastSessionViewBuilder id(SessionId id) {
            this.id = id;
            return this;
        }

        public PastSessionViewBuilder title(SessionTitle title) {
            this.title = title;
            return this;
        }

        public PastSessionViewBuilder opening(OpeningPrompt opening) {
            this.opening = opening;
            return this;
        }

        public PastSessionViewBuilder reading(TranscriptReading reading) {
            this.reading = reading;
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

        public PastSessionViewBuilder interrupted(boolean interrupted) {
            this.interrupted = interrupted;
            return this;
        }

        public PastSessionView build() {
            return new PastSessionView(id, title, opening, reading, directory, lastActivity, age, startedAt, links, interrupted);
        }
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.ElsewhereGroup;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.PlaceName;

/** The live sessions of one place outside the project, strongest marker first, under the name of the project or directory. */
public record ElsewhereGroupView(PlaceName name, List<LiveSessionView> sessions) {

    static ElsewhereGroupView from(AvailableSessions available, ElsewhereGroup group) {
        return builder()
                .name(group.place().name())
                .sessions(group.sessions().stream()
                        .map(session -> LiveSessionView.from(available, session))
                        .toList())
                .build();
    }

    /** The strongest marker among the sessions that are blocked on you; none when the place is at most at your turn. */
    public Optional<Marker> strongestWaiting() {
        return Marker.strongest(sessions.stream()
                .flatMap(session -> session.marker().stream())
                .filter(Marker::isWaiting)
                .toList());
    }

    public static ElsewhereGroupViewBuilder builder() {
        return new ElsewhereGroupViewBuilder();
    }

    public static final class ElsewhereGroupViewBuilder {
        private PlaceName name;
        private List<LiveSessionView> sessions;

        private ElsewhereGroupViewBuilder() {}

        public ElsewhereGroupViewBuilder name(PlaceName name) {
            this.name = name;
            return this;
        }

        public ElsewhereGroupViewBuilder sessions(List<LiveSessionView> sessions) {
            this.sessions = sessions;
            return this;
        }

        public ElsewhereGroupView build() {
            return new ElsewhereGroupView(name, sessions);
        }
    }
}

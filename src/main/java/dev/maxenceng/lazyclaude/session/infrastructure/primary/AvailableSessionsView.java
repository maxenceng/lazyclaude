package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.published.Marker;

/** The project's sessions: the live ones newest start first, then the past ones newest activity first. */
public record AvailableSessionsView(List<LiveSessionView> live, List<PastSessionView> past) implements SessionsView {

    static AvailableSessionsView from(AvailableSessions available) {
        return builder()
                .live(available.live().stream()
                        .map(session -> LiveSessionView.from(available, session))
                        .toList())
                .past(available.past().stream()
                        .map(session -> PastSessionView.from(available, session))
                        .toList())
                .build();
    }

    /** The strongest marker among this project's live sessions, linked or not. */
    public Optional<Marker> strongestMarker() {
        return Marker.strongest(live.stream().flatMap(session -> session.marker().stream()).toList());
    }

    /** The strongest marker among the live sessions linked to {@code ticket}, weak links included. */
    public Optional<Marker> strongestMarkerOf(String ticket) {
        return strongestSessionOf(ticket).flatMap(LiveSessionView::marker);
    }

    /** The live session linked to {@code ticket}, weak links included, with the strongest marker; the first of any tied. */
    public Optional<LiveSessionView> strongestSessionOf(String ticket) {
        return live.stream()
                .filter(session -> session.linkTo(ticket).isPresent() && session.marker().isPresent())
                .reduce((best, next) -> next.marker().orElseThrow().compareTo(best.marker().orElseThrow()) > 0 ? next : best);
    }

    /** The live sessions linked to no ticket. */
    public List<LiveSessionView> unlinkedLive() {
        return live.stream().filter(LiveSessionView::isUnlinked).toList();
    }

    /** The past sessions linked to no ticket, newest activity first. */
    public List<PastSessionView> unlinkedPast() {
        return past.stream().filter(PastSessionView::isUnlinked).toList();
    }

    public static AvailableSessionsViewBuilder builder() {
        return new AvailableSessionsViewBuilder();
    }

    public static final class AvailableSessionsViewBuilder {
        private List<LiveSessionView> live;
        private List<PastSessionView> past;

        private AvailableSessionsViewBuilder() {}

        public AvailableSessionsViewBuilder live(List<LiveSessionView> live) {
            this.live = live;
            return this;
        }

        public AvailableSessionsViewBuilder past(List<PastSessionView> past) {
            this.past = past;
            return this;
        }

        public AvailableSessionsView build() {
            return new AvailableSessionsView(live, past);
        }
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Marker;

/** The project's sessions: the live ones newest start first, those elsewhere by place, then the past ones newest activity first, and the hosted ones in tab order. */
public record AvailableSessionsView(
        List<LiveSessionView> live,
        List<ElsewhereGroupView> elsewhere,
        List<PastSessionView> past,
        List<HostedSessionView> hosted)
        implements SessionsView {

    static AvailableSessionsView from(AvailableSessions available) {
        return builder()
                .live(available.live().stream()
                        .map(session -> LiveSessionView.from(available, session))
                        .toList())
                .elsewhere(available.elsewhere().stream()
                        .map(group -> ElsewhereGroupView.from(available, group))
                        .toList())
                .past(available.past().stream()
                        .map(session -> PastSessionView.from(available, session))
                        .toList())
                .hosted(available.hosted().stream()
                        .map(tab -> HostedSessionView.from(available, tab))
                        .toList())
                .build();
    }

    /** How many of this project's live sessions, linked or not, carry each marker; a marker none carries is absent. */
    public Map<Marker, Long> markerCounts() {
        return live.stream()
                .flatMap(session -> session.marker().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /** The strongest marker among the live sessions linked to {@code ticket}, weak links included. */
    public Optional<Marker> strongestMarkerOf(LinkedTicketId ticket) {
        return strongestSessionOf(ticket).flatMap(LiveSessionView::marker);
    }

    /** The live session linked to {@code ticket}, weak links included, with the strongest marker; the first of any tied. */
    public Optional<LiveSessionView> strongestSessionOf(LinkedTicketId ticket) {
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
        private List<ElsewhereGroupView> elsewhere = List.of();
        private List<PastSessionView> past;
        private List<HostedSessionView> hosted = List.of();

        private AvailableSessionsViewBuilder() {}

        public AvailableSessionsViewBuilder live(List<LiveSessionView> live) {
            this.live = live;
            return this;
        }

        public AvailableSessionsViewBuilder elsewhere(List<ElsewhereGroupView> elsewhere) {
            this.elsewhere = elsewhere;
            return this;
        }

        public AvailableSessionsViewBuilder past(List<PastSessionView> past) {
            this.past = past;
            return this;
        }

        public AvailableSessionsViewBuilder hosted(List<HostedSessionView> hosted) {
            this.hosted = hosted;
            return this;
        }

        public AvailableSessionsView build() {
            return new AvailableSessionsView(live, elsewhere, past, hosted);
        }
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.TicketLink;

/**
 * The project's sessions: the hosted live ones newest start first, then the past ones newest activity first, and the hosted ones
 * in tab order; then, from those, the sessions each ticket lists and the ones linked to no ticket, so neither frontend decides
 * which sessions a ticket lists.
 */
public record SessionsView(
        List<LiveSessionView> live,
        List<PastSessionView> past,
        List<HostedSessionView> hosted,
        List<TicketSessionsView> tickets,
        UnlinkedSessionsView unlinked) {

    public static SessionsView from(AvailableSessions available) {
        return builder()
                .live(available.live().stream()
                        .map(session -> LiveSessionView.from(available, session))
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

    /** The strongest marker among the live sessions linked to {@code ticket}. */
    public Optional<Marker> strongestMarkerOf(LinkedTicketId ticket) {
        return strongestSessionOf(ticket).flatMap(LiveSessionView::marker);
    }

    /** The live session linked to {@code ticket}, with the strongest marker; the first of any tied. */
    public Optional<LiveSessionView> strongestSessionOf(LinkedTicketId ticket) {
        return live.stream()
                .filter(session -> session.linkTo(ticket).isPresent() && session.marker().isPresent())
                .max(Comparator.comparing(session -> session.marker().orElse(Marker.YOUR_TURN)));
    }

    /** The sessions {@code ticket} lists, none when no session is linked to it or hosted for it. */
    public TicketSessionsView sessionsOf(LinkedTicketId ticket) {
        return tickets.stream()
                .filter(listed -> listed.ticket().equals(ticket))
                .findFirst()
                .orElseGet(() -> TicketSessionsView.builder().ticket(ticket).build());
    }

    public static SessionsViewBuilder builder() {
        return new SessionsViewBuilder();
    }

    public static final class SessionsViewBuilder {
        private List<LiveSessionView> live;
        private List<PastSessionView> past;
        private List<HostedSessionView> hosted = List.of();

        private SessionsViewBuilder() {}

        public SessionsViewBuilder live(List<LiveSessionView> live) {
            this.live = live;
            return this;
        }

        public SessionsViewBuilder past(List<PastSessionView> past) {
            this.past = past;
            return this;
        }

        public SessionsViewBuilder hosted(List<HostedSessionView> hosted) {
            this.hosted = hosted;
            return this;
        }

        public SessionsView build() {
            List<TicketSessionsView> tickets = Stream.of(
                            live.stream().flatMap(session -> session.links().stream()).map(TicketLink::ticket),
                            past.stream().flatMap(session -> session.links().stream()).map(TicketLink::ticket),
                            hosted.stream().flatMap(tab -> tab.ticket().stream()))
                    .flatMap(Function.identity())
                    .distinct()
                    .map(ticket -> TicketSessionsView.of(ticket, live, past, hosted))
                    .toList();
            return new SessionsView(live, past, hosted, tickets, UnlinkedSessionsView.of(live, past));
        }
    }
}

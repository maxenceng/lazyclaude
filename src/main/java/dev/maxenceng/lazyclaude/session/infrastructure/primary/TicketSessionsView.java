package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/**
 * The sessions a ticket lists, and none other: the live and past ones linked to it, in the project's order, and the hosted
 * tabs started for it or running a session linked to it, a resumed one included, in tab order. Each frontend lays them out; which belong is this view's.
 */
public record TicketSessionsView(
        LinkedTicketId ticket, List<LiveSessionView> live, List<PastSessionView> past, List<HostedSessionView> hosted) {

    static TicketSessionsView of(
            LinkedTicketId ticket, List<LiveSessionView> live, List<PastSessionView> past, List<HostedSessionView> hosted) {
        return builder()
                .ticket(ticket)
                .live(live.stream().filter(session -> session.linkTo(ticket).isPresent()).toList())
                .past(past.stream().filter(session -> session.linkTo(ticket).isPresent()).toList())
                .hosted(hosted.stream()
                        .filter(tab -> tab.ticket().filter(ticket::equals).isPresent()
                                || tab.live().flatMap(session -> session.linkTo(ticket)).isPresent())
                        .toList())
                .build();
    }

    public static TicketSessionsViewBuilder builder() {
        return new TicketSessionsViewBuilder();
    }

    public static final class TicketSessionsViewBuilder {
        private LinkedTicketId ticket;
        private List<LiveSessionView> live = List.of();
        private List<PastSessionView> past = List.of();
        private List<HostedSessionView> hosted = List.of();

        private TicketSessionsViewBuilder() {}

        public TicketSessionsViewBuilder ticket(LinkedTicketId ticket) {
            this.ticket = ticket;
            return this;
        }

        public TicketSessionsViewBuilder live(List<LiveSessionView> live) {
            this.live = live;
            return this;
        }

        public TicketSessionsViewBuilder past(List<PastSessionView> past) {
            this.past = past;
            return this;
        }

        public TicketSessionsViewBuilder hosted(List<HostedSessionView> hosted) {
            this.hosted = hosted;
            return this;
        }

        public TicketSessionsView build() {
            return new TicketSessionsView(ticket, live, past, hosted);
        }
    }
}

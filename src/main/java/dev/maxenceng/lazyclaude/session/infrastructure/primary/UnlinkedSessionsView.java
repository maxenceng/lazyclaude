package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

/** The sessions linked to no ticket, listed only while no ticket is selected: the live ones, then the past ones newest activity first. */
public record UnlinkedSessionsView(List<LiveSessionView> live, List<PastSessionView> past) {

    static UnlinkedSessionsView of(List<LiveSessionView> live, List<PastSessionView> past) {
        return builder()
                .live(live.stream().filter(session -> session.links().isEmpty()).toList())
                .past(past.stream().filter(session -> session.links().isEmpty()).toList())
                .build();
    }

    public static UnlinkedSessionsViewBuilder builder() {
        return new UnlinkedSessionsViewBuilder();
    }

    public static final class UnlinkedSessionsViewBuilder {
        private List<LiveSessionView> live = List.of();
        private List<PastSessionView> past = List.of();

        private UnlinkedSessionsViewBuilder() {}

        public UnlinkedSessionsViewBuilder live(List<LiveSessionView> live) {
            this.live = live;
            return this;
        }

        public UnlinkedSessionsViewBuilder past(List<PastSessionView> past) {
            this.past = past;
            return this;
        }

        public UnlinkedSessionsView build() {
            return new UnlinkedSessionsView(live, past);
        }
    }
}

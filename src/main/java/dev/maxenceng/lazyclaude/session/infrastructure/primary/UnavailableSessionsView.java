package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.HostedTab;
import dev.maxenceng.lazyclaude.session.domain.UnavailableSessions;
import dev.maxenceng.lazyclaude.session.published.Reason;

/** Sessions that could not be read, and why, with the hosted ones, which lazyclaude still holds. */
public record UnavailableSessionsView(Reason reason, List<HostedSessionView> hosted) implements SessionsView {

    static UnavailableSessionsView from(UnavailableSessions sessions) {
        return builder()
                .reason(sessions.reason())
                .hosted(sessions.hosted().stream().map(UnavailableSessionsView::tab).toList())
                .build();
    }

    private static HostedSessionView tab(HostedTab tab) {
        return HostedSessionView.builder()
                .id(tab.session().id())
                .pid(tab.session().pid())
                .busyOrWaiting(tab.phase().isBusyOrWaiting())
                .build();
    }

    public static UnavailableSessionsViewBuilder builder() {
        return new UnavailableSessionsViewBuilder();
    }

    public static final class UnavailableSessionsViewBuilder {
        private Reason reason;
        private List<HostedSessionView> hosted = List.of();

        private UnavailableSessionsViewBuilder() {}

        public UnavailableSessionsViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnavailableSessionsViewBuilder hosted(List<HostedSessionView> hosted) {
            this.hosted = hosted;
            return this;
        }

        public UnavailableSessionsView build() {
            return new UnavailableSessionsView(reason, hosted);
        }
    }
}

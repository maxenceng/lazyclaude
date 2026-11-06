package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.StatusVerbs;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.maxenceng.lazyclaude.session.published.TicketPlugin;

/** What the user may set: the chords reserved inside a hosted session, and whether a waiting session shows a desktop notification, whether the terminal's mouse is captured, and the plugin a ticket's new session runs, and the verbs it gives the statuses it names; a status it leaves out is not in the list. */
public record Configuration(
        ReservedChords reservedChords, boolean desktopNotifications, boolean mouse, TicketPlugin ticketPlugin, List<StatusVerbs> statusVerbs) {

    public Configuration {
        Assert.notNull("reserved chords", reservedChords);
        Assert.notNull("ticket plugin", ticketPlugin);
        Assert.notNull("status verbs", statusVerbs);
    }

    public static ConfigurationBuilder builder() {
        return new ConfigurationBuilder();
    }

    public static final class ConfigurationBuilder {
        private ReservedChords reservedChords;
        private Boolean desktopNotifications;
        private Boolean mouse;
        private TicketPlugin ticketPlugin;
        private List<StatusVerbs> statusVerbs = List.of();

        private ConfigurationBuilder() {}

        public ConfigurationBuilder reservedChords(ReservedChords reservedChords) {
            this.reservedChords = reservedChords;
            return this;
        }

        public ConfigurationBuilder desktopNotifications(boolean desktopNotifications) {
            this.desktopNotifications = desktopNotifications;
            return this;
        }

        public ConfigurationBuilder mouse(boolean mouse) {
            this.mouse = mouse;
            return this;
        }

        public ConfigurationBuilder ticketPlugin(TicketPlugin ticketPlugin) {
            this.ticketPlugin = ticketPlugin;
            return this;
        }

        public ConfigurationBuilder statusVerbs(List<StatusVerbs> statusVerbs) {
            this.statusVerbs = statusVerbs;
            return this;
        }

        public Configuration build() {
            Assert.notNull("desktopNotifications", desktopNotifications);
            Assert.notNull("mouse", mouse);
            return new Configuration(reservedChords, desktopNotifications, mouse, ticketPlugin, statusVerbs);
        }
    }
}

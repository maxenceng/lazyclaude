package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.Configuration;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

/** What the user configured: the chords the shell takes inside a hosted session, and whether a waiting session shows a desktop notification, and whether the shell captures the mouse. */
public record ConfigurationView(List<ReservedChordView> chords, boolean desktopNotifications, boolean mouse) {

    /** One chord per reserved action in {@link ReservedAction}'s order, an action left out keeping its default, whatever list it is given. */
    public ConfigurationView {
        Assert.notNull("chords", chords);
        chords = chordsOf(boundIn(chords));
    }

    /** The chords as the domain holds them, which a frontend that matches presses in-process reads; the protocol writes {@link #chords}. */
    public ReservedChords reservedChords() {
        return boundIn(chords);
    }

    private static ReservedChords boundIn(List<ReservedChordView> chords) {
        refuseTwice(chords);
        return new ReservedChords(chords.stream().collect(Collectors.toMap(ReservedChordView::action, ReservedChordView::chord)));
    }

    private static void refuseTwice(List<ReservedChordView> chords) {
        Set<ReservedAction> seen = EnumSet.noneOf(ReservedAction.class);
        chords.stream()
                .map(ReservedChordView::action)
                .filter(action -> !seen.add(action))
                .findFirst()
                .ifPresent(action -> {
                    throw new ReservedActionTwiceException(action);
                });
    }

    private static List<ReservedChordView> chordsOf(ReservedChords reserved) {
        return Arrays.stream(ReservedAction.values())
                .map(action -> ReservedChordView.builder().action(action).chord(reserved.of(action)).build())
                .toList();
    }

    static ConfigurationView from(Configuration configuration) {
        return builder()
                .reservedChords(configuration.reservedChords())
                .desktopNotifications(configuration.desktopNotifications())
                .mouse(configuration.mouse())
                .build();
    }

    public static ConfigurationViewBuilder builder() {
        return new ConfigurationViewBuilder();
    }

    public static final class ConfigurationViewBuilder {
        private ReservedChords reservedChords;
        private Boolean desktopNotifications;
        private Boolean mouse;

        private ConfigurationViewBuilder() {}

        public ConfigurationViewBuilder reservedChords(ReservedChords reservedChords) {
            this.reservedChords = reservedChords;
            return this;
        }

        public ConfigurationViewBuilder desktopNotifications(boolean desktopNotifications) {
            this.desktopNotifications = desktopNotifications;
            return this;
        }

        public ConfigurationViewBuilder mouse(boolean mouse) {
            this.mouse = mouse;
            return this;
        }

        public ConfigurationView build() {
            Assert.notNull("desktopNotifications", desktopNotifications);
            Assert.notNull("mouse", mouse);
            Assert.notNull("reservedChords", reservedChords);
            return new ConfigurationView(chordsOf(reservedChords), desktopNotifications, mouse);
        }
    }
}

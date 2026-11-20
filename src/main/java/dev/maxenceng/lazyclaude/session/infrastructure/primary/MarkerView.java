package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Marker;

/**
 * A live session's marker and how strongly it asks of you, the greater the stronger. The strength is the marker's place in
 * {@link Marker}'s declaration, so the ranking stays written once: the desktop frontend orders by it rather than by a table of its own,
 * the builder takes the kind alone, and the constructor refuses any other strength. Strength is relative: compare two, never one with a
 * constant.
 */
public record MarkerView(Marker kind, int strength) {

    public MarkerView {
        Assert.notNull("kind", kind);
        if (strength != kind.ordinal()) {
            throw new MarkerStrengthNotItsRankException(kind, strength);
        }
    }

    static MarkerView of(Marker marker) {
        return builder().kind(marker).build();
    }

    public static MarkerViewBuilder builder() {
        return new MarkerViewBuilder();
    }

    public static final class MarkerViewBuilder {
        private Marker kind;

        private MarkerViewBuilder() {}

        public MarkerViewBuilder kind(Marker kind) {
            this.kind = kind;
            return this;
        }

        public MarkerView build() {
            Assert.notNull("kind", kind);
            return new MarkerView(kind, kind.ordinal());
        }
    }
}

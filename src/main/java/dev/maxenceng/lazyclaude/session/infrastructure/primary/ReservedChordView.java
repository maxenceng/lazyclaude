package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Chord;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;

/** The chord one reserved action is bound to, with the modifiers and the key a frontend matches a key press against. */
public record ReservedChordView(ReservedAction action, Chord chord) {

    public static ReservedChordViewBuilder builder() {
        return new ReservedChordViewBuilder();
    }

    public static final class ReservedChordViewBuilder {
        private ReservedAction action;
        private Chord chord;

        private ReservedChordViewBuilder() {}

        public ReservedChordViewBuilder action(ReservedAction action) {
            this.action = action;
            return this;
        }

        public ReservedChordViewBuilder chord(Chord chord) {
            this.chord = chord;
            return this;
        }

        public ReservedChordView build() {
            Assert.notNull("action", action);
            Assert.notNull("chord", chord);
            return new ReservedChordView(action, chord);
        }
    }
}

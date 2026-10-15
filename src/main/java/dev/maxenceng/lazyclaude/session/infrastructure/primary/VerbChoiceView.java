package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.VerbChoice;

/** The verbs a ticket allows, in the order they are offered, and the one under the cursor when the picker opens. */
public record VerbChoiceView(List<Verb> verbs, Verb preselected) {

    static VerbChoiceView from(VerbChoice choice) {
        return builder().verbs(choice.verbs()).preselected(choice.preselected()).build();
    }

    public static VerbChoiceViewBuilder builder() {
        return new VerbChoiceViewBuilder();
    }

    public static final class VerbChoiceViewBuilder {
        private List<Verb> verbs;
        private Verb preselected;

        private VerbChoiceViewBuilder() {}

        public VerbChoiceViewBuilder verbs(List<Verb> verbs) {
            this.verbs = verbs;
            return this;
        }

        public VerbChoiceViewBuilder preselected(Verb preselected) {
            this.preselected = preselected;
            return this;
        }

        public VerbChoiceView build() {
            return new VerbChoiceView(verbs, preselected);
        }
    }
}

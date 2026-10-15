package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The verbs a ticket allows, in the order they are offered, and the one under the cursor when the choice opens. */
public record VerbChoice(List<Verb> verbs, Verb preselected) {

    public VerbChoice {
        Assert.notEmpty("verbs", verbs);
        Assert.notNull("preselected verb", preselected);
    }

    /**
     * The choice among {@code allowed}: {@code design} while no session has run it, else {@code done} once the ticket branch is merged,
     * else the first verb.
     */
    public static VerbChoice of(List<Verb> allowed, boolean designRun, boolean merged) {
        Assert.notEmpty("verbs", allowed);
        return builder()
                .verbs(allowed)
                .preselected(preselect(allowed, designRun, merged))
                .build();
    }

    private static Verb preselect(List<Verb> allowed, boolean designRun, boolean merged) {
        if (allowed.contains(Verb.DESIGN) && !designRun) {
            return Verb.DESIGN;
        }
        if (allowed.contains(Verb.DONE) && merged) {
            return Verb.DONE;
        }
        return allowed.getFirst();
    }

    public static VerbChoiceBuilder builder() {
        return new VerbChoiceBuilder();
    }

    public static final class VerbChoiceBuilder {
        private List<Verb> verbs;
        private Verb preselected;

        private VerbChoiceBuilder() {}

        public VerbChoiceBuilder verbs(List<Verb> verbs) {
            this.verbs = verbs;
            return this;
        }

        public VerbChoiceBuilder preselected(Verb preselected) {
            this.preselected = preselected;
            return this;
        }

        public VerbChoice build() {
            return new VerbChoice(verbs, preselected);
        }
    }
}

package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;

public final class VerbChoiceViewFixture {

    private VerbChoiceViewFixture() {}

    public static VerbChoiceView designPreselected() {
        return VerbChoiceView.builder()
                .verbs(List.of(Verb.REVIEW, Verb.DESIGN))
                .preselected(Verb.DESIGN)
                .build();
    }

    public static VerbChoiceView reviewPreselected() {
        return VerbChoiceView.builder()
                .verbs(List.of(Verb.REVIEW, Verb.DESIGN))
                .preselected(Verb.REVIEW)
                .build();
    }

    public static VerbChoiceView respondPreselected() {
        return VerbChoiceView.builder()
                .verbs(List.of(Verb.RESPOND, Verb.DONE))
                .preselected(Verb.RESPOND)
                .build();
    }

    public static VerbChoiceView donePreselected() {
        return VerbChoiceView.builder()
                .verbs(List.of(Verb.RESPOND, Verb.DONE))
                .preselected(Verb.DONE)
                .build();
    }

    public static VerbChoiceView refinePreselected() {
        return VerbChoiceView.builder().verbs(List.of(Verb.REFINE)).preselected(Verb.REFINE).build();
    }
}

package dev.maxenceng.lazyclaude.session.published;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * A readable transcript's title and what it opened with, either of which it may lack. The title falls back to the opening
 * prompt, so the two are drawn together through {@link #of} rather than handed in apart.
 */
public record ReadFacts(SessionTitle title, OpeningPrompt opening) implements SessionFacts {

    public ReadFacts {
        Assert.notNull("title", title);
        Assert.notNull("opening prompt", opening);
    }

    /** The facts of a transcript that holds {@code aiTitle} and {@code firstPrompt}, each of which it may lack. */
    public static ReadFacts of(Optional<String> aiTitle, Optional<String> firstPrompt) {
        return new ReadFacts(
                SessionTitle.of(aiTitle, firstPrompt),
                firstPrompt.<OpeningPrompt>map(FirstPrompt::new).orElseGet(NoFirstPrompt::new));
    }
}

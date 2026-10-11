package dev.maxenceng.lazyclaude.session.published;

import java.util.Optional;

/** What a past session is called on its row: the title Claude Code gave it, else what was first asked, else nothing. */
public sealed interface SessionTitle permits AiTitle, FirstPrompt, NoTitle {

    /** The title a session goes by, from what its transcript holds, in that order of preference. */
    static SessionTitle of(Optional<String> aiTitle, Optional<String> firstPrompt) {
        return aiTitle.<SessionTitle>map(AiTitle::new)
                .or(() -> firstPrompt.map(FirstPrompt::new))
                .orElseGet(NoTitle::new);
    }
}

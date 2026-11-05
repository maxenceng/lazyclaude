package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.session.domain.NewSession;
import dev.maxenceng.lazyclaude.session.domain.UnlinkedStart;
import dev.maxenceng.lazyclaude.session.domain.VerbChoice;

/** What starting a session from a ticket opens: the verb picker over its verbs, or an unlinked session when it allows none. */
public sealed interface NewSessionView permits VerbChoiceView, UnlinkedStartView {

    static NewSessionView from(NewSession session) {
        return switch (session) {
            case VerbChoice choice -> VerbChoiceView.from(choice);
            case UnlinkedStart _ -> new UnlinkedStartView();
        };
    }
}

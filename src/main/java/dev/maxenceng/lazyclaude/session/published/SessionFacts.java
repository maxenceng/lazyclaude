package dev.maxenceng.lazyclaude.session.published;

/** What a past session's transcript says of it, or why it could not be read. */
public sealed interface SessionFacts permits ReadFacts, UnreadableFacts {

    /** What the session is called on its row; a transcript that could not be read names nothing. */
    SessionTitle title();
}

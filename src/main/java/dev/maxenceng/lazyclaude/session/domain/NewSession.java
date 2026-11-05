package dev.maxenceng.lazyclaude.session.domain;

/** What starting a session from a ticket opens: the choice among the verbs it allows, or an unlinked session when it allows none. */
public sealed interface NewSession permits VerbChoice, UnlinkedStart {}

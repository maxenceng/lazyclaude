package dev.maxenceng.lazyclaude.docs.domain;

/** What a project's {@code docs/} holds as the Docs panel shows it: its tree, or the reason it could not be listed. */
public sealed interface Docs permits DocsTree, UnavailableDocs {}

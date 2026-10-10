package dev.maxenceng.lazyclaude.docs.domain;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;

/** One file under {@code docs/} that the tree lists: read into text, or refused with a reason. */
public sealed interface Document permits ReadDocument, UnreadableDocument {

    DocumentPath path();
}

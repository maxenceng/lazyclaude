package dev.maxenceng.lazyclaude.session.published;

/** What a past session was first asked, or that its transcript holds no prompt. */
public sealed interface OpeningPrompt permits FirstPrompt, NoFirstPrompt {}

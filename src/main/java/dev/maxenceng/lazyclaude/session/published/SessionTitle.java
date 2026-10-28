package dev.maxenceng.lazyclaude.session.published;

/** What a past session is called on its row: the title Claude Code gave it, else what was first asked, else nothing. */
public sealed interface SessionTitle permits AiTitle, FirstPrompt, NoTitle {}

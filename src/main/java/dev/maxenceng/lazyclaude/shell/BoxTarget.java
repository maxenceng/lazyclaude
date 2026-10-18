package dev.maxenceng.lazyclaude.shell;

/** What a comment box writes: a new comment, the text of a pending one, a reply to a thread, or the summary. */
sealed interface BoxTarget permits NewCommentTarget, EditCommentTarget, ReplyTarget, SummaryTarget {}

package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A backlog file with an `id` whose frontmatter or status was refused, or whose `id` was already claimed. */
public record UnreadableTicket(String file, String reason) implements BacklogEntry {

    public UnreadableTicket {
        Assert.notNull("file", file);
        Assert.notNull("reason", reason);
    }
}

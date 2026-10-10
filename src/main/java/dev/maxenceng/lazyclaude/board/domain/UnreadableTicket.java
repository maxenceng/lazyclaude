package dev.maxenceng.lazyclaude.board.domain;

import java.util.Objects;

/** A backlog file with an `id` whose frontmatter or status was refused, or whose `id` was already claimed. */
public record UnreadableTicket(String file, String reason) {

    public UnreadableTicket {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(reason, "reason");
    }
}

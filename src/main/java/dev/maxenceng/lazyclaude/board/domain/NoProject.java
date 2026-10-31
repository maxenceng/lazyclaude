package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A working directory that belongs to no project, or whose project could not be told: no board, and the reason why. */
public record NoProject(Reason reason) implements BoardLookup {

    public NoProject {
        Assert.notNull("reason", reason);
    }
}

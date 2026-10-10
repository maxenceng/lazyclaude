package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why a backlog file could not be read as a ticket, written for the person reading the board. */
public record Reason(String value) {

    public Reason {
        Assert.notBlank("reason", value);
    }
}

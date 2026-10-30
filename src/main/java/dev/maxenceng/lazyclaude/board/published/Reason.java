package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why a ticket could not be read, or the board not at all, written for the person reading the board. */
public record Reason(String value) {

    public Reason {
        Assert.notBlank("reason", value);
    }
}

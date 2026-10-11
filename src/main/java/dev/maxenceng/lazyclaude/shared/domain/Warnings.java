package dev.maxenceng.lazyclaude.shared.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Every warning raised so far, oldest first; there is always at least one, since none raised is nothing to show. */
public record Warnings(List<Warning> raised) {

    public Warnings {
        Assert.notEmpty("warnings", raised);
    }

    public Warning newest() {
        return raised.getLast();
    }

    public int count() {
        return raised.size();
    }
}
